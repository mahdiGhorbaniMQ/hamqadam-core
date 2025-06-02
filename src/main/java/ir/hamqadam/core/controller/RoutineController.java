package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.common.PageableResponseDTO;
import ir.hamqadam.core.controller.dto.routine.*;
// import ir.hamqadam.core.controller.dto.user.UserSummaryDTO; // Needed for enriching participant DTO
// import ir.hamqadam.core.controller.dto.team.TeamSummaryDTO; // Needed for enriching participant DTO
import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.model.Routine;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.RoutineService;
import ir.hamqadam.core.service.UserService;
// import ir.hamqadam.core.service.TeamService; // If enriching team participant details

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
// import org.modelmapper.ModelMapper;

@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {

    private final RoutineService routineService;
    private final UserService userService;
    // private final TeamService teamService; // For enriching participant DTOs
    // private final ModelMapper modelMapper;

    @Autowired
    public RoutineController(RoutineService routineService, UserService userService /*, TeamService teamService, ModelMapper modelMapper */) {
        this.routineService = routineService;
        this.userService = userService;
        // this.teamService = teamService;
        // this.modelMapper = modelMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoutineResponseDTO> createRoutine(
            @Valid @RequestBody RoutineCreationRequestDTO creationRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User actingUser = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));

        Routine.CreatorInfo modelCreatorInfo = new Routine.CreatorInfo(
                creationRequest.getCreatorInfo().getCreatorType(),
                creationRequest.getCreatorInfo().getCreatorId(),
                actingUser.getUserId()
        );

        Routine newRoutine = routineService.createRoutine(
                creationRequest.getTitle(),
                creationRequest.getDescriptivePostId(),
                modelCreatorInfo,
                creationRequest.getScheduleType(),
                creationRequest.getStartDatetime(),
                creationRequest.getEndDatetime(),
                creationRequest.getRecurrenceRule(),
                creationRequest.getDuration(),
                creationRequest.getTimezone(),
                creationRequest.getPurposeOrGoal(),
                creationRequest.getLocationOrPlatformDetails(),
                creationRequest.getInitialParticipants(),
                creationRequest.getVisibility(),
                creationRequest.getInitialStatus(),
                creationRequest.getLinkedProjectId(),
                creationRequest.getLinkedTeamId(),
                actingUser
        );
        // Add tasks and reminders if they are part of creation DTO and service method
        // For now, assuming service handles them if they are in model via builder.
        // newRoutine.setRoutineTasksOrActions(creationRequest.getRoutineTasksOrActions());
        // newRoutine.setReminderRules(creationRequest.getReminderRules());
        // Routine savedRoutine = routineRepository.save(newRoutine); // Service should save

        return new ResponseEntity<>(convertToRoutineResponseDTO(newRoutine), HttpStatus.CREATED);
    }

    @GetMapping("/{routineId}")
    public ResponseEntity<RoutineResponseDTO> getRoutineById(@PathVariable String routineId) {
        Routine routine = routineService.findRoutineById(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", "ID", routineId));
        // Add visibility checks based on current user (if authenticated)
        return ResponseEntity.ok(convertToRoutineResponseDTO(routine));
    }

    @PutMapping("/{routineId}")
    @PreAuthorize("@routineSecurityService.canUpdateRoutine(#routineId, principal.username)")
    public ResponseEntity<RoutineResponseDTO> updateRoutineInfo(
            @PathVariable String routineId,
            @Valid @RequestBody RoutineUpdateRequestDTO updateRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Routine updatedRoutine = routineService.updateRoutineInfo(
                routineId,
                updateRequest.getTitle(),
                updateRequest.getDescriptivePostId(),
                updateRequest.getScheduleType(),
                updateRequest.getStartDatetime(),
                updateRequest.getEndDatetime(),
                updateRequest.getRecurrenceRule(),
                updateRequest.getDuration(),
                updateRequest.getTimezone(),
                updateRequest.getPurposeOrGoal(),
                updateRequest.getLocationOrPlatformDetails(),
                updateRequest.getVisibility(),
                currentUserDetails.getUsername() // or User ID
        );
        // Handle updates to tasks and reminders if they are part of the DTO
        // This often involves more complex logic like merging or replacing collections.
        // For now, assuming service handles this based on DTO fields.
        return ResponseEntity.ok(convertToRoutineResponseDTO(updatedRoutine));
    }

    @PutMapping("/{routineId}/status")
    @PreAuthorize("@routineSecurityService.canChangeRoutineStatus(#routineId, principal.username)")
    public ResponseEntity<RoutineResponseDTO> changeRoutineStatus(
            @PathVariable String routineId,
            @Valid @RequestBody RoutineStatusUpdateRequestDTO statusRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Routine updatedRoutine = routineService.changeRoutineStatus(
                routineId,
                statusRequest.getNewStatus(),
                currentUserDetails.getUsername() // or User ID
        );
        return ResponseEntity.ok(convertToRoutineResponseDTO(updatedRoutine));
    }

    @PostMapping("/{routineId}/participants")
    @PreAuthorize("@routineSecurityService.canManageRoutineParticipants(#routineId, principal.username)")
    public ResponseEntity<RoutineResponseDTO> addParticipant(
            @PathVariable String routineId,
            @Valid @RequestBody RoutineParticipantManagementRequestDTO participantRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Routine.RoutineParticipant participant = Routine.RoutineParticipant.builder()
                .participantType(participantRequest.getParticipantType())
                .participantId(participantRequest.getParticipantId())
                .roleInRoutine(participantRequest.getRoleInRoutine())
                .optional(participantRequest.getOptional() != null ? participantRequest.getOptional() : false)
                .invitationStatus(Routine.InvitationStatus.INVITED) // Default to invited when added by admin
                .build();
        Routine updatedRoutine = routineService.addParticipantToRoutine(routineId, participant, currentUserDetails.getUsername());
        return ResponseEntity.ok(convertToRoutineResponseDTO(updatedRoutine));
    }

    @DeleteMapping("/{routineId}/participants/{participantType}/{participantId}")
    @PreAuthorize("@routineSecurityService.canManageRoutineParticipants(#routineId, principal.username)")
    public ResponseEntity<RoutineResponseDTO> removeParticipant(
            @PathVariable String routineId,
            @PathVariable Routine.ParticipantType participantType,
            @PathVariable String participantId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Routine updatedRoutine = routineService.removeParticipantFromRoutine(routineId, participantId, participantType, currentUserDetails.getUsername());
        return ResponseEntity.ok(convertToRoutineResponseDTO(updatedRoutine));
    }

    @PostMapping("/{routineId}/participants/{participantType}/{participantId}/rsvp")
    @PreAuthorize("isAuthenticated()") // Participant themselves or an admin can RSVP
    public ResponseEntity<RoutineResponseDTO> rsvpToRoutine(
            @PathVariable String routineId,
            @PathVariable Routine.ParticipantType participantType,
            @PathVariable String participantId,
            @Valid @RequestBody RoutineRsvpRequestDTO rsvpRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        // Security check: ensure currentUserDetails.getUsername() matches participantId if USER type,
        // OR currentUserDetails has admin rights on the routine.
        // This logic should be in the @routineSecurityService or service layer.
        Routine updatedRoutine = routineService.updateParticipantRsvpStatus(
                routineId, participantId, participantType, rsvpRequest.getRsvpStatus(), currentUserDetails.getUsername());
        return ResponseEntity.ok(convertToRoutineResponseDTO(updatedRoutine));
    }

    @GetMapping("/my-routines")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageableResponseDTO<RoutineResponseDTO>> getMyRoutines(
            @AuthenticationPrincipal UserDetails currentUserDetails,
            @PageableDefault(size = 10, sort = "nextOccurrenceDatetime") Pageable pageable) {
        User user = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        Page<Routine> routinePage = routineService.findRoutinesByParticipantUser(user.getUserId(), pageable);
        Page<RoutineResponseDTO> dtoPage = routinePage.map(this::convertToRoutineResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<PageableResponseDTO<RoutineResponseDTO>> getRoutinesByProject(
            @PathVariable String projectId,
            @PageableDefault(size = 10, sort = "nextOccurrenceDatetime") Pageable pageable) {
        Page<Routine> routinePage = routineService.findRoutinesByLinkedProject(projectId, pageable);
        Page<RoutineResponseDTO> dtoPage = routinePage.map(this::convertToRoutineResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    @GetMapping("/feed") // Example: upcoming public routines
    public ResponseEntity<PageableResponseDTO<RoutineResponseDTO>> getUpcomingPublicRoutines(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 10, sort = "nextOccurrenceDatetime") Pageable pageable) {
        Page<Routine> routinePage = routineService.findRoutinesByNextOccurrenceBetween(from, to, Optional.of(Routine.RoutineStatus.ACTIVE), pageable);
        // Further filter for PUBLIC visibility if not handled by service query
        List<RoutineResponseDTO> publicRoutines = routinePage.getContent().stream()
                .filter(r -> r.getVisibility() == Routine.RoutineVisibility.PUBLIC)
                .map(this::convertToRoutineResponseDTO)
                .collect(Collectors.toList());

        // Re-create PageableResponseDTO if filtering happened in controller
        return ResponseEntity.ok(new PageableResponseDTO<>(
                publicRoutines,
                routinePage.getNumber(),
                routinePage.getSize(),
                (long)publicRoutines.size(), // This would be incorrect if filtering reduced items from a full page.
                // Better to handle visibility filtering in the service/repository for correct pagination.
                publicRoutines.isEmpty() ? 0 : 1, // Incorrect total pages
                true, false, publicRoutines.size(), publicRoutines.isEmpty() // Simplified pagination fields if filtered here
        ));
    }

    // --- Helper method for DTO conversion (Placeholder) ---
    private RoutineResponseDTO convertToRoutineResponseDTO(Routine routine) {
        if (routine == null) return null;
        // Use ModelMapper or MapStruct

        List<RoutineParticipantDTO> participantDTOs = routine.getParticipants() == null ? null :
                routine.getParticipants().stream().map(p -> {
                    RoutineParticipantDTO.RoutineParticipantDTOBuilder participantBuilder = RoutineParticipantDTO.builder()
                            .participantType(p.getParticipantType())
                            .participantId(p.getParticipantId())
                            .roleInRoutine(p.getRoleInRoutine())
                            .invitationStatus(p.getInvitationStatus())
                            .optional(p.isOptional());
                    // Enrich with participant name and picture URL
                    // This part needs to fetch User/Team details, which can be N+1.
                    // Consider batch fetching or a more optimized approach.
                    // if (p.getParticipantType() == Routine.ParticipantType.USER) {
                    //     userService.findUserById(p.getParticipantId()).ifPresent(u -> {
                    //         participantBuilder.participantName(u.getFullName() != null ? u.getFullName().get("en") : null);
                    //         participantBuilder.participantProfilePictureUrl(u.getProfilePictures() != null && !u.getProfilePictures().isEmpty() ? u.getProfilePictures().get(0).getUrl() : null);
                    //     });
                    // } else if (p.getParticipantType() == Routine.ParticipantType.TEAM_REPRESENTATIVE) {
                    //     teamService.findTeamById(p.getParticipantId()).ifPresent(t -> {
                    //         participantBuilder.participantName(t.getTeamName() != null ? t.getTeamName().get("en") : null);
                    //         participantBuilder.participantProfilePictureUrl(t.getProfilePictureUrl());
                    //     });
                    // }
                    return participantBuilder.build();
                }).collect(Collectors.toList());

        List<RoutineTaskOrActionDTO> taskDTOs = routine.getRoutineTasksOrActions() == null ? null :
                routine.getRoutineTasksOrActions().stream().map(task -> RoutineTaskOrActionDTO.builder()
                        .taskTitle(task.getTaskTitle())
                        .taskDescription(task.getTaskDescription())
                        .assignedToRoleInRoutine(task.getAssignedToRoleInRoutine())
                        .assignedToUserId(task.getAssignedToUserId())
                        .dueCondition(task.getDueCondition())
                        // .status(...) // Status might be instance-specific, not part of definition
                        .build()
                ).collect(Collectors.toList());

        return RoutineResponseDTO.builder()
                .routineId(routine.getRoutineId())
                .title(routine.getTitle())
                .descriptivePostId(routine.getDescriptivePostId())
                .creatorInfo(routine.getCreatorInfo())
                .status(routine.getStatus())
                .visibility(routine.getVisibility())
                .scheduleType(routine.getScheduleType())
                .startDatetime(routine.getStartDatetime())
                .endDatetime(routine.getEndDatetime())
                .recurrenceRule(routine.getRecurrenceRule())
                .duration(routine.getDuration())
                .timezone(routine.getTimezone())
                .nextOccurrenceDatetime(routine.getNextOccurrenceDatetime())
                .purposeOrGoal(routine.getPurposeOrGoal())
                .agendaTemplate(routine.getAgendaTemplate())
                .locationOrPlatformDetails(routine.getLocationOrPlatformDetails())
                .externalLink(routine.getExternalLink())
                .participants(participantDTOs)
                .routineTasksOrActions(taskDTOs)
                .reminderRules(routine.getReminderRules())
                .linkedProjectId(routine.getLinkedProjectId())
                .linkedTeamId(routine.getLinkedTeamId())
                .createdAt(routine.getCreatedAt())
                .updatedAt(routine.getUpdatedAt())
                .build();
    }
}