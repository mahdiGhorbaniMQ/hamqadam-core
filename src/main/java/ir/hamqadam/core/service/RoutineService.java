package ir.hamqadam.core.service;

import ir.hamqadam.core.model.Routine;
import ir.hamqadam.core.model.User; // For actingUser context
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RoutineService {

    /**
     * Creates a new routine.
     * The descriptive post must be created separately and its ID provided.
     *
     * @param title                 i18n map of the routine title.
     * @param descriptivePostId     ID of the mandatory Post describing the routine.
     * @param creatorInfo           Information about the creator (User or Team).
     * @param scheduleType          Type of schedule (RECURRING, SINGLE_OCCURRENCE).
     * @param startDatetime         Start datetime of the first/single occurrence.
     * @param endDatetime           Optional end datetime for the routine or single occurrence.
     * @param recurrenceRule        iCalendar RRULE string for recurring routines.
     * @param duration              Duration of each occurrence (e.g., "PT1H30M").
     * @param timezone              Timezone for the schedule.
     * @param purposeOrGoal         i18n map for the routine's purpose.
     * @param locationOrPlatform    Details about the location or online platform.
     * @param initialParticipants   List of initial participants.
     * @param visibility            Visibility of the routine.
     * @param initialStatus         Initial status of the routine.
     * @param linkedProjectId       Optional ID of a project this routine is linked to.
     * @param linkedTeamId          Optional ID of a team this routine is primarily for.
     * @param actingUser            The user performing the creation.
     * @return The created Routine object.
     * @throws ir.hamqadam.core.exception.ValidationException if data is invalid.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if related entities not found.
     */
    Routine createRoutine(Map<String, String> title,
                          String descriptivePostId,
                          Routine.CreatorInfo creatorInfo,
                          Routine.ScheduleType scheduleType,
                          LocalDateTime startDatetime,
                          LocalDateTime endDatetime,
                          String recurrenceRule,
                          String duration,
                          String timezone,
                          Map<String, String> purposeOrGoal,
                          Routine.LocationOrPlatformDetails locationOrPlatform,
                          List<Routine.RoutineParticipant> initialParticipants,
                          Routine.RoutineVisibility visibility,
                          Routine.RoutineStatus initialStatus,
                          String linkedProjectId,
                          String linkedTeamId,
                          User actingUser);

    /**
     * Finds a routine by its ID.
     *
     * @param routineId The ID of the routine.
     * @return An Optional containing the routine if found.
     */
    Optional<Routine> findRoutineById(String routineId);

    /**
     * Updates an existing routine's information.
     *
     * @param routineId         The ID of the routine to update.
     * @param updatedFields     A Routine object or DTO containing fields to update.
     * (For this example, specific updatable fields are listed).
     * @param actingUserId      ID of the user performing the update.
     * @return The updated Routine object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if routine not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Routine updateRoutineInfo(String routineId,
                              Map<String, String> title, // i18n
                              // ... other updatable fields like schedule, purpose, location, visibility
                              String descriptivePostId, // If allowed to change
                              Routine.ScheduleType scheduleType,
                              LocalDateTime startDatetime,
                              LocalDateTime endDatetime,
                              String recurrenceRule,
                              String duration,
                              String timezone,
                              Map<String, String> purposeOrGoal,
                              Routine.LocationOrPlatformDetails locationOrPlatform,
                              Routine.RoutineVisibility visibility,
                              String actingUserId);

    /**
     * Changes the status of a routine (e.g., ACTIVE -> PAUSED, ACTIVE -> COMPLETED).
     *
     * @param routineId    The ID of the routine.
     * @param newStatus    The new status for the routine.
     * @param actingUserId ID of the user performing the action.
     * @return The updated Routine object.
     */
    Routine changeRoutineStatus(String routineId, Routine.RoutineStatus newStatus, String actingUserId);

    /**
     * Adds a participant to a routine.
     *
     * @param routineId        The ID of the routine.
     * @param participant      The participant details to add.
     * @param actingUserId     ID of the user performing the action.
     * @return The updated Routine object.
     */
    Routine addParticipantToRoutine(String routineId, Routine.RoutineParticipant participant, String actingUserId);

    /**
     * Removes a participant from a routine.
     *
     * @param routineId        The ID of the routine.
     * @param participantId    The ID of the participant (UserId or TeamId, based on participantType).
     * @param participantType  The type of the participant.
     * @param actingUserId     ID of the user performing the action.
     * @return The updated Routine object.
     */
    Routine removeParticipantFromRoutine(String routineId, String participantId, Routine.ParticipantType participantType, String actingUserId);

    /**
     * Updates a participant's RSVP status for a routine.
     *
     * @param routineId      The ID of the routine.
     * @param participantId  The ID of the participant.
     * @param participantType The type of participant.
     * @param newStatus      The new invitation status (ACCEPTED, DECLINED, TENTATIVE).
     * @param actingUserId   ID of the user performing the action (should be the participant themselves or an organizer).
     * @return The updated Routine object.
     */
    Routine updateParticipantRsvpStatus(String routineId, String participantId, Routine.ParticipantType participantType, Routine.InvitationStatus newStatus, String actingUserId);

    /**
     * Retrieves routines where the given user is a participant.
     * @param userId The ID of the user.
     * @param pageable Pagination information.
     * @return A Page of routines.
     */
    Page<Routine> findRoutinesByParticipantUser(String userId, Pageable pageable);

    /**
     * Retrieves routines linked to a specific project.
     * @param projectId The ID of the project.
     * @param pageable Pagination information.
     * @return A Page of routines.
     */
    Page<Routine> findRoutinesByLinkedProject(String projectId, Pageable pageable);

    /**
     * Retrieves routines linked to a specific team (either as creator or explicitly linked).
     * @param teamId The ID of the team.
     * @param pageable Pagination information.
     * @return A Page of routines.
     */
    Page<Routine> findRoutinesByLinkedTeam(String teamId, Pageable pageable);

    /**
     * Finds routines scheduled for their next occurrence within a given time window.
     * @param fromDateTime Start of the time window.
     * @param toDateTime End of the time window.
     * @param status Optional filter by routine status.
     * @param pageable Pagination information.
     * @return A Page of routines.
     */
    Page<Routine> findRoutinesByNextOccurrenceBetween(LocalDateTime fromDateTime, LocalDateTime toDateTime, Optional<Routine.RoutineStatus> status, Pageable pageable);

    /**
     * (Internal or Admin task) Recalculates the next occurrence for a recurring routine.
     * This might be called periodically or after a routine's schedule is updated.
     * @param routineId The ID of the routine.
     * @return The updated routine with recalculated next_occurrence_datetime.
     */
    Routine recalculateNextOccurrence(String routineId);

    // Methods for managing RoutineTaskOrAction might be added here if they are complex.
    // For Phase 1, they might be managed as part of updateRoutineInfo if simple,
    // or tasks are handled by a separate system/concept.
}