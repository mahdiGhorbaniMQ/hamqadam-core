package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.Routine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
// Import @Query if needed for more complex queries
// import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
// import java.util.List; // Already imported if needed previously

@Repository
public interface RoutineRepository extends MongoRepository<Routine, String> {

    /**
     * Finds routines by their status.
     *
     * @param status   The routine status.
     * @param pageable Pagination information.
     * @return A page of routines with the specified status.
     */
    Page<Routine> findByStatus(Routine.RoutineStatus status, Pageable pageable);

    /**
     * Finds routines created by a specific user or team.
     *
     * @param creatorType The type of creator (USER or TEAM).
     * @param creatorId   The ID of the creator.
     * @param pageable    Pagination information.
     * @return A page of routines created by the specified creator.
     */
    Page<Routine> findByCreatorInfo_CreatorTypeAndCreatorInfo_CreatorId(
            Routine.CreatorType creatorType, String creatorId, Pageable pageable);

    /**
     * Finds routines linked to a specific project.
     *
     * @param projectId The ID of the linked project.
     * @param pageable  Pagination information.
     * @return A page of routines linked to the specified project.
     */
    Page<Routine> findByLinkedProjectId(String projectId, Pageable pageable);

    /**
     * Finds routines linked to a specific team (either as explicitly linked or as the creator).
     * This method combines both conditions.
     *
     * @param linkedTeamId The ID of the team if it's in the linkedTeamId field.
     * @param creatorId    The ID of the team if it's the creator (should be same as linkedTeamId in this call).
     * @param creatorType  Should be Routine.CreatorType.TEAM.
     * @param pageable     Pagination information.
     * @return A page of routines linked to or created by the specified team.
     */
    // For more complex OR conditions, a @Query might be more readable or necessary
    // However, Spring Data can sometimes derive this if the property paths are distinct enough.
    // If this derived query becomes problematic, a custom @Query would be the solution.
    // Example using derived query (might need testing for OR complexity):
    Page<Routine> findByLinkedTeamIdOrCreatorInfo_CreatorIdAndCreatorInfo_CreatorType(
            String linkedTeamId, String creatorId, Routine.CreatorType creatorType, Pageable pageable);

    // Simpler alternative if the above derived query is problematic or if you want to query by linkedTeamId separately:
    /**
     * Finds routines explicitly linked to a specific team.
     * @param teamId The ID of the linked team.
     * @param pageable Pagination information.
     * @return A page of routines linked to the specified team.
     */
    Page<Routine> findByLinkedTeamId(String teamId, Pageable pageable);


    /**
     * Finds routines where a specific user is a participant.
     *
     * @param userId   The ID of the user participant.
     * @param participantType The type of participant (USER).
     * @param pageable Pagination information.
     * @return A page of routines where the user is a participant.
     */
    Page<Routine> findByParticipants_ParticipantIdAndParticipants_ParticipantType(
            String userId, Routine.ParticipantType participantType, Pageable pageable);

    /**
     * Finds routines with the next occurrence before a certain date and time and with a specific status.
     *
     * @param nextOccurrenceDateTime The date and time to compare against.
     * @param status                 The status of the routine.
     * @param pageable               Pagination information.
     * @return A page of routines with upcoming occurrences.
     */
    Page<Routine> findByNextOccurrenceDatetimeBeforeAndStatus(
            LocalDateTime nextOccurrenceDateTime, Routine.RoutineStatus status, Pageable pageable);

    /**
     * Finds routines with the next occurrence within a given date range and with a specific status.
     * @param startTime Start of the date range.
     * @param endTime End of the date range.
     * @param status The status of the routine.
     * @param pageable Pagination information.
     * @return A page of routines.
     */
    Page<Routine> findByNextOccurrenceDatetimeBetweenAndStatus(
            LocalDateTime startTime, LocalDateTime endTime, Routine.RoutineStatus status, Pageable pageable);
}