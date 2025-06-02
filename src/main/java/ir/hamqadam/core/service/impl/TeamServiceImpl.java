package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.exception.UnauthorizedException;
import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.TeamRepository;
import ir.hamqadam.core.repository.UserRepository;
// import ir.hamqadam.core.service.NotificationService; // For sending notifications
import ir.hamqadam.core.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize; // For method-level security
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamServiceImpl.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    // private final NotificationService notificationService; // Autowire if using

    // For Phase 1, define admin role string
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBER = "MEMBER";


    @Autowired
    public TeamServiceImpl(TeamRepository teamRepository, UserRepository userRepository
            /*, NotificationService notificationService */) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        // this.notificationService = notificationService;
    }

    @Override
    public Team createTeam(Map<String, String> teamName,
                           String teamHandle,
                           String introductoryPostId,
                           Map<String, String> description,
                           Team.TeamVisibility visibility,
                           boolean membershipApprovalRequired,
                           User actingUser) {

        if (!StringUtils.hasText(teamHandle) || teamRepository.existsByTeamHandle(teamHandle)) {
            throw new ValidationException("Team handle is invalid or already exists: " + teamHandle);
        }
        if (introductoryPostId == null) { // Assuming post service would have validated post existence
            throw new ValidationException("Introductory post ID is required.");
        }

        Team.TeamMember creatorAsMember = Team.TeamMember.builder()
                .userId(actingUser.getUserId())
                .roles(Collections.singletonList(ROLE_ADMIN)) // Creator is the first admin
                .joinDate(LocalDateTime.now())
                .statusInTeam(Team.MemberStatus.ACTIVE)
                .build();

        Team newTeam = Team.builder()
                .teamName(teamName)
                .teamHandle(teamHandle)
                .introductoryPostId(introductoryPostId)
                .description(description)
                .visibility(visibility)
                .membershipApprovalRequired(membershipApprovalRequired)
                .creatorUserId(actingUser.getUserId())
                .members(Collections.singletonList(creatorAsMember))
                .teamStatus(Team.TeamStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        logger.info("Creating new team '{}' by user '{}'", teamHandle, actingUser.getUserId());
        return teamRepository.save(newTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findTeamById(String teamId) {
        return teamRepository.findById(teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findTeamByHandle(String teamHandle) {
        return teamRepository.findByTeamHandle(teamHandle);
    }

    @Override
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)") // Custom security expression
    public Team updateTeamInfo(String teamId,
                               Map<String, String> teamName,
                               Map<String, String> description,
                               String profilePictureUrl,
                               String coverPictureUrl,
                               Team.TeamVisibility visibility,
                               boolean membershipApprovalRequired,
                               String actingUserId) { // actingUserId already available via principal
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));

        // Permission check: actingUserId must be an admin of this team
        // This can be done via @PreAuthorize or custom logic here
        // checkAdminPermission(team, actingUserId);


        if (teamName != null) team.setTeamName(teamName);
        if (description != null) team.setDescription(description);
        if (profilePictureUrl != null) team.setProfilePictureUrl(profilePictureUrl);
        if (coverPictureUrl != null) team.setCoverPictureUrl(coverPictureUrl);
        if (visibility != null) team.setVisibility(visibility);
        team.setMembershipApprovalRequired(membershipApprovalRequired); // boolean can be directly set

        team.setUpdatedAt(LocalDateTime.now());
        logger.info("Team '{}' info updated by user '{}'", teamId, actingUserId);
        return teamRepository.save(team);
    }

    private void checkAdminPermission(Team team, String actingUserId) {
        boolean isAdmin = team.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(actingUserId) &&
                        member.getRoles().contains(ROLE_ADMIN) &&
                        member.getStatusInTeam() == Team.MemberStatus.ACTIVE);
        if (!isAdmin) {
            throw new UnauthorizedException("User " + actingUserId + " is not authorized to perform this action on team " + team.getTeamId());
        }
    }


    @Override
    @PreAuthorize("@teamSecurityService.canInviteToTeam(#teamId, principal.username)")
    public Team inviteUserToTeam(String teamId, String userIdToInvite, List<String> rolesForUser, String actingUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));
        userRepository.findById(userIdToInvite)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userIdToInvite));

        // checkAdminPermission(team, actingUserId); // Or more granular invite permission

        boolean alreadyMemberOrInvited = team.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(userIdToInvite));
        if (alreadyMemberOrInvited) {
            throw new ValidationException("User is already a member or invited to team " + teamId);
        }

        Team.TeamMember invitedMember = Team.TeamMember.builder()
                .userId(userIdToInvite)
                .roles(rolesForUser == null || rolesForUser.isEmpty() ? Collections.singletonList(ROLE_MEMBER) : rolesForUser)
                .statusInTeam(Team.MemberStatus.INVITED)
                .joinDate(null) // Join date will be set upon acceptance
                .build();

        if (team.getMembers() == null) {
            team.setMembers(new ArrayList<>());
        }
        team.getMembers().add(invitedMember);
        team.setUpdatedAt(LocalDateTime.now());

        // notificationService.sendTeamInvitation(userIdToInvite, teamId, actingUserId);
        logger.info("User '{}' invited to team '{}' by user '{}'", userIdToInvite, teamId, actingUserId);
        return teamRepository.save(team);
    }

    @Override
    public Team respondToTeamInvitation(String teamId, String invitedUserId, boolean accept) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));

        Team.TeamMember member = team.getMembers().stream()
                .filter(m -> m.getUserId().equals(invitedUserId) && m.getStatusInTeam() == Team.MemberStatus.INVITED)
                .findFirst()
                .orElseThrow(() -> new ValidationException("No pending invitation found for user " + invitedUserId + " in team " + teamId));

        if (accept) {
            member.setStatusInTeam(Team.MemberStatus.ACTIVE);
            member.setJoinDate(LocalDateTime.now());
            logger.info("User '{}' accepted invitation to team '{}'", invitedUserId, teamId);
            // notificationService.notifyTeamAdmins(teamId, "User " + invitedUserId + " accepted invitation.");
        } else {
            team.getMembers().remove(member); // Or mark as DECLINED if you want to keep history
            logger.info("User '{}' declined invitation to team '{}'", invitedUserId, teamId);
        }
        team.setUpdatedAt(LocalDateTime.now());
        return teamRepository.save(team);
    }

    @Override
    public Team requestToJoinTeam(String teamId, String requestingUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", requestingUserId));

        if (team.getVisibility() != Team.TeamVisibility.PUBLIC) {
            throw new ValidationException("Team " + teamId + " is not public.");
        }
        if (!team.isMembershipApprovalRequired()) {
            throw new ValidationException("Team " + teamId + " does not require membership approval for joining (or is open join).");
        }

        boolean alreadyMemberOrRequested = team.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(requestingUserId));
        if (alreadyMemberOrRequested) {
            throw new ValidationException("User " + requestingUserId + " is already a member or has a pending request for team " + teamId);
        }

        Team.TeamMember requestingMember = Team.TeamMember.builder()
                .userId(requestingUserId)
                .roles(Collections.singletonList(ROLE_MEMBER)) // Default role upon approval
                .statusInTeam(Team.MemberStatus.PENDING_APPROVAL)
                .build();

        if (team.getMembers() == null) {
            team.setMembers(new ArrayList<>());
        }
        team.getMembers().add(requestingMember);
        team.setUpdatedAt(LocalDateTime.now());

        // notificationService.notifyTeamAdmins(teamId, "User " + requestingUserId + " requested to join.");
        logger.info("User '{}' requested to join team '{}'", requestingUserId, teamId);
        return teamRepository.save(team);
    }

    @Override
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)")
    public Team processMembershipRequest(String teamId, String userIdToProcess, boolean approve, String actingUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));

        // checkAdminPermission(team, actingUserId);

        Team.TeamMember member = team.getMembers().stream()
                .filter(m -> m.getUserId().equals(userIdToProcess) && m.getStatusInTeam() == Team.MemberStatus.PENDING_APPROVAL)
                .findFirst()
                .orElseThrow(() -> new ValidationException("No pending membership request found for user " + userIdToProcess + " in team " + teamId));

        if (approve) {
            member.setStatusInTeam(Team.MemberStatus.ACTIVE);
            member.setJoinDate(LocalDateTime.now());
            logger.info("Membership request for user '{}' approved for team '{}' by '{}'", userIdToProcess, teamId, actingUserId);
            // notificationService.notifyUser(userIdToProcess, "Your request to join team " + team.getTeamName() + " was approved.");
        } else {
            team.getMembers().remove(member); // Or mark as REJECTED
            logger.info("Membership request for user '{}' rejected for team '{}' by '{}'", userIdToProcess, teamId, actingUserId);
            // notificationService.notifyUser(userIdToProcess, "Your request to join team " + team.getTeamName() + " was rejected.");
        }
        team.setUpdatedAt(LocalDateTime.now());
        return teamRepository.save(team);
    }

    @Override
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)")
    public Team updateTeamMemberRoles(String teamId, String memberUserId, List<String> newRoles, String actingUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));

        // checkAdminPermission(team, actingUserId);

        Team.TeamMember member = team.getMembers().stream()
                .filter(m -> m.getUserId().equals(memberUserId) && m.getStatusInTeam() == Team.MemberStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Active member", "userId", memberUserId + " in team " + teamId));

        if (newRoles == null || newRoles.isEmpty()) {
            throw new ValidationException("Roles list cannot be empty.");
        }
        // Add validation for allowed roles if needed (e.g., from team.definedTeamRoles)

        member.setRoles(new ArrayList<>(newRoles)); // Ensure it's a mutable list if needed
        team.setUpdatedAt(LocalDateTime.now());
        logger.info("Roles updated for member '{}' in team '{}' by user '{}'", memberUserId, teamId, actingUserId);
        // notificationService.notifyUser(memberUserId, "Your roles in team " + team.getTeamName() + " have been updated.");
        return teamRepository.save(team);
    }

    @Override
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username) or #memberUserId == principal.username") // Admin or self (for leave)
    public Team removeTeamMember(String teamId, String memberUserId, String actingUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));

        Team.TeamMember memberToRemove = team.getMembers().stream()
                .filter(m -> m.getUserId().equals(memberUserId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Member", "userId", memberUserId + " in team " + teamId));

        // More complex permission check:
        // If actingUser is removing self, it's 'leaveTeam'.
        // If actingUser is removing someone else, they must be admin.
        boolean isSelfLeave = actingUserId.equals(memberUserId);
        if (!isSelfLeave) {
            checkAdminPermission(team, actingUserId);
        }

        // Prevent removing the last admin if they are not also the last member
        if (memberToRemove.getRoles().contains(ROLE_ADMIN)) {
            long adminCount = team.getMembers().stream()
                    .filter(m -> m.getRoles().contains(ROLE_ADMIN) && m.getStatusInTeam() == Team.MemberStatus.ACTIVE)
                    .count();
            if (adminCount <= 1 && team.getMembers().size() > 1) { // If last admin and other members exist
                throw new ValidationException("Cannot remove the last admin from team " + teamId + " if other members exist. Promote another member first.");
            }
        }

        team.getMembers().remove(memberToRemove);
        team.setUpdatedAt(LocalDateTime.now());
        logger.info("Member '{}' removed from team '{}' by user '{}'", memberUserId, teamId, actingUserId);
        // notificationService.notifyUser(memberUserId, "You have been removed from team " + team.getTeamName());
        // if (!isSelfLeave) notificationService.notifyTeamAdmins(teamId, "Member " + memberUserId + " was removed by " + actingUserId);
        return teamRepository.save(team);
    }

    @Override
    public Team leaveTeam(String teamId, String memberUserId) {
        // This can call removeTeamMember with memberUserId as actingUserId
        return removeTeamMember(teamId, memberUserId, memberUserId);
    }

    @Override
    @PreAuthorize("@teamSecurityService.isTeamAdmin(#teamId, principal.username)")
    public Team changeTeamStatus(String teamId, Team.TeamStatus newStatus, String actingUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));

        // checkAdminPermission(team, actingUserId);

        team.setTeamStatus(newStatus);
        team.setUpdatedAt(LocalDateTime.now());
        logger.info("Team '{}' status changed to {} by user '{}'", teamId, newStatus, actingUserId);
        // Potentially notify all team members
        return teamRepository.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    // @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')") // Example for system-wide listing
    public Page<Team> findAllTeams(Pageable pageable) {
        // Could add filter for only ACTIVE teams for regular users, or all for admins
        return teamRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Team> findTeamsByMemberUserId(String userId, Pageable pageable) {
        return teamRepository.findByMembers_UserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Team> searchPublicTeams(String query, Pageable pageable) {
        // For Phase 1, basic search. Enhance with text search or more specific criteria later.
        if (!StringUtils.hasText(query)) {
            return teamRepository.findByVisibility(Team.TeamVisibility.PUBLIC, pageable);
        }
        // Example: (This is a naive search, not efficient for i18n or large datasets)
        // This requires appropriate methods in TeamRepository
        // return teamRepository.findByVisibilityAndTeamNameContainingIgnoreCase(Team.TeamVisibility.PUBLIC, query, pageable);
        logger.warn("SearchPublicTeams basic implementation used. Query: {}", query);
        return teamRepository.findByVisibility(Team.TeamVisibility.PUBLIC, pageable); // Placeholder
    }
}