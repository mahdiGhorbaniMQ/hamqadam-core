package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.common.PageableResponseDTO;
import ir.hamqadam.core.controller.dto.team.*;
import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.TeamService;
import ir.hamqadam.core.service.UserService; // For fetching actingUser details

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
// import org.modelmapper.ModelMapper; // If using ModelMapper

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;
    private final UserService userService; // To get User object from UserDetails
    // private final ModelMapper modelMapper;

    @Autowired
    public TeamController(TeamService teamService, UserService userService /*, ModelMapper modelMapper */) {
        this.teamService = teamService;
        this.userService = userService;
        // this.modelMapper = modelMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponseDTO> createTeam(
            @Valid @RequestBody TeamCreationRequestDTO creationRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User actingUser = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));

        Team newTeam = teamService.createTeam(
                creationRequest.getTeamName(),
                creationRequest.getTeamHandle(),
                creationRequest.getIntroductoryPostId(),
                creationRequest.getDescription(),
                creationRequest.getVisibility(),
                creationRequest.isMembershipApprovalRequired(),
                actingUser
        );
        return new ResponseEntity<>(convertToTeamResponseDTO(newTeam), HttpStatus.CREATED);
    }

    @GetMapping("/{teamIdOrHandle}")
    public ResponseEntity<TeamResponseDTO> getTeamDetails(@PathVariable String teamIdOrHandle) {
        Team team = teamService.findTeamById(teamIdOrHandle) // Attempt by ID
                .orElseGet(() -> teamService.findTeamByHandle(teamIdOrHandle) // Attempt by Handle
                        .orElseThrow(() -> new ResourceNotFoundException("Team", "identifier", teamIdOrHandle)));
        return ResponseEntity.ok(convertToTeamResponseDTO(team));
    }

    @PutMapping("/{teamId}")
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)")
    public ResponseEntity<TeamResponseDTO> updateTeamInfo(
            @PathVariable String teamId,
            @Valid @RequestBody TeamUpdateRequestDTO updateRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Team updatedTeam = teamService.updateTeamInfo(
                teamId,
                updateRequest.getTeamName(),
                updateRequest.getDescription(),
                updateRequest.getProfilePictureUrl(),
                updateRequest.getCoverPictureUrl(),
                updateRequest.getVisibility(),
                updateRequest.getMembershipApprovalRequired() != null ? updateRequest.getMembershipApprovalRequired() : false, // Provide a default if null
                currentUserDetails.getUsername() // Or fetch User ID
        );
        return ResponseEntity.ok(convertToTeamResponseDTO(updatedTeam));
    }

    @PostMapping("/{teamId}/invitations")
    @PreAuthorize("@teamSecurityService.canInviteToTeam(#teamId, principal.username)")
    public ResponseEntity<MessageResponse> inviteUserToTeam(
            @PathVariable String teamId,
            @Valid @RequestBody TeamInviteRequestDTO inviteRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        teamService.inviteUserToTeam(
                teamId,
                inviteRequest.getUserIdToInvite(),
                inviteRequest.getRoles(),
                currentUserDetails.getUsername() // Or fetch User ID
        );
        return ResponseEntity.ok(new MessageResponse("User invited successfully to team " + teamId));
    }

    @PostMapping("/{teamId}/invitations/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> respondToTeamInvitation(
            @PathVariable String teamId,
            @Valid @RequestBody TeamJoinDecisionRequestDTO decisionRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User user = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        teamService.respondToTeamInvitation(teamId, user.getUserId(), decisionRequest.getAccept());
        String message = decisionRequest.getAccept() ? "Invitation accepted." : "Invitation declined.";
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @PostMapping("/{teamId}/join-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> requestToJoinTeam(
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User user = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        teamService.requestToJoinTeam(teamId, user.getUserId());
        return ResponseEntity.ok(new MessageResponse("Request to join team " + teamId + " submitted."));
    }

    @PostMapping("/{teamId}/join-requests/{requestingUserId}/process")
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)")
    public ResponseEntity<MessageResponse> processMembershipRequest(
            @PathVariable String teamId,
            @PathVariable String requestingUserId,
            @Valid @RequestBody TeamJoinDecisionRequestDTO decisionRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        teamService.processMembershipRequest(
                teamId,
                requestingUserId,
                decisionRequest.getAccept(),
                currentUserDetails.getUsername() // Or fetch User ID
        );
        String message = decisionRequest.getAccept() ? "Membership request approved." : "Membership request rejected.";
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @PutMapping("/{teamId}/members/{memberUserId}/roles")
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)")
    public ResponseEntity<TeamResponseDTO> updateTeamMemberRoles(
            @PathVariable String teamId,
            @PathVariable String memberUserId,
            @Valid @RequestBody TeamMemberRoleUpdateRequestDTO roleUpdateRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Team updatedTeam = teamService.updateTeamMemberRoles(
                teamId,
                memberUserId,
                roleUpdateRequest.getNewRoles(),
                currentUserDetails.getUsername() // Or fetch User ID
        );
        return ResponseEntity.ok(convertToTeamResponseDTO(updatedTeam));
    }

    @DeleteMapping("/{teamId}/members/{memberUserId}")
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)")
    public ResponseEntity<MessageResponse> removeTeamMember(
            @PathVariable String teamId,
            @PathVariable String memberUserId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        teamService.removeTeamMember(
                teamId,
                memberUserId,
                currentUserDetails.getUsername() // Or fetch User ID
        );
        return ResponseEntity.ok(new MessageResponse("Member " + memberUserId + " removed from team " + teamId));
    }

    @PostMapping("/{teamId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> leaveTeam(
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User user = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        teamService.leaveTeam(teamId, user.getUserId());
        return ResponseEntity.ok(new MessageResponse("You have left team " + teamId));
    }

    @PutMapping("/{teamId}/status")
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)") // Or a higher role like SYSTEM_ADMIN
    public ResponseEntity<TeamResponseDTO> changeTeamStatus(
            @PathVariable String teamId,
            @Valid @RequestBody TeamStatusUpdateRequestDTO statusUpdateRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Team updatedTeam = teamService.changeTeamStatus(
                teamId,
                statusUpdateRequest.getNewStatus(),
                currentUserDetails.getUsername() // Or fetch User ID
        );
        return ResponseEntity.ok(convertToTeamResponseDTO(updatedTeam));
    }

    @GetMapping("/my-teams")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageableResponseDTO<TeamResponseDTO>> getMyTeams(
            @AuthenticationPrincipal UserDetails currentUserDetails,
            @PageableDefault(size = 10, sort = "teamName.en") Pageable pageable) {
        User user = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        Page<Team> teamPage = teamService.findTeamsByMemberUserId(user.getUserId(), pageable);
        Page<TeamResponseDTO> dtoPage = teamPage.map(this::convertToTeamResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    @GetMapping("/search")
    public ResponseEntity<PageableResponseDTO<TeamResponseDTO>> searchPublicTeams(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "teamName.en") Pageable pageable) {
        Page<Team> teamPage = teamService.searchPublicTeams(query, pageable);
        Page<TeamResponseDTO> dtoPage = teamPage.map(this::convertToTeamResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    // --- Helper method for DTO conversion (Placeholder) ---
    private TeamResponseDTO convertToTeamResponseDTO(Team team) {
        if (team == null) return null;
        // In a real app, use ModelMapper, MapStruct, or dedicated mapper classes.
        // This also needs to fetch user details for member DTOs if they are not directly in Team.Member
        List<TeamMemberDTO> memberDTOs = team.getMembers() == null ? null :
                team.getMembers().stream().map(member -> {
                    // Fetch user details to get full name and profile picture for TeamMemberDTO
                    // This is an N+1 problem if not handled carefully.
                    // For simplicity here, assume you might fetch them or have them denormalized.
                    // User memberUser = userService.findUserById(member.getUserId()).orElse(null);
                    return TeamMemberDTO.builder()
                            .userId(member.getUserId())
                            // .userFullName(memberUser != null ? (memberUser.getFullName() != null ? memberUser.getFullName().get("en") : "N/A") : "N/A") // Example
                            // .userProfilePictureUrl(memberUser != null ? (memberUser.getProfilePictures() != null && !memberUser.getProfilePictures().isEmpty() ? memberUser.getProfilePictures().get(0).getUrl() : null) : null) // Example
                            .roles(member.getRoles())
                            .joinDate(member.getJoinDate())
                            .statusInTeam(member.getStatusInTeam())
                            .build();
                }).collect(Collectors.toList());

        return TeamResponseDTO.builder()
                .teamId(team.getTeamId())
                .teamName(team.getTeamName())
                .teamHandle(team.getTeamHandle())
                .creatorUserId(team.getCreatorUserId())
                .introductoryPostId(team.getIntroductoryPostId())
                .description(team.getDescription())
                .profilePictureUrl(team.getProfilePictureUrl())
                .coverPictureUrl(team.getCoverPictureUrl())
                .members(memberDTOs)
                .visibility(team.getVisibility())
                .membershipApprovalRequired(team.isMembershipApprovalRequired())
                .teamStatus(team.getTeamStatus())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                // .projectCount( ... ) // Could be fetched or denormalized
                // .routineCount( ... )
                .build();
    }
}