package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.exception.UnauthorizedException;
import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.Routine;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.RoutineRepository;
import ir.hamqadam.core.repository.UserRepository;
import ir.hamqadam.core.repository.TeamRepository;
import ir.hamqadam.core.repository.PostRepository;
// import ir.hamqadam.core.service.NotificationService;
import ir.hamqadam.core.service.RoutineService;
// import some.library.for.rrule.parser.RRule; // For parsing iCalendar RRULE
// import some.library.for.rrule.parser.RecurrenceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Transactional
public class RoutineServiceImpl implements RoutineService {

    private static final Logger logger = LoggerFactory.getLogger(RoutineServiceImpl.class);

    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PostRepository postRepository;
    // private final NotificationService notificationService;
    // private final RRuleParserService rruleParserService; // A hypothetical service for RRULE

    @Autowired
    public RoutineServiceImpl(RoutineRepository routineRepository,
                              UserRepository userRepository,
                              TeamRepository teamRepository,
                              PostRepository postRepository
            /*, NotificationService notificationService, RRuleParserService rruleParserService */) {
        this.routineRepository = routineRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.postRepository = postRepository;
        // this.notificationService = notificationService;
        // this.rruleParserService = rruleParserService;
    }

    @Override
    public Routine createRoutine(Map<String, String> title,
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
                                 String linkedProjectId, // Optional
                                 String linkedTeamId,    // Optional
                                 User actingUser) {

        if (!StringUtils.hasText(descriptivePostId)) {
            throw new ValidationException("Descriptive Post ID is required for creating a routine.");
        }
        postRepository.findById(descriptivePostId)
                .orElseThrow(() -> new ResourceNotFoundException("Post (descriptive)", "ID", descriptivePostId));

        // Validate creator
        if (creatorInfo.getCreatorType() == Routine.CreatorType.USER) {
            userRepository.findById(creatorInfo.getCreatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("User (creator)", "ID", creatorInfo.getCreatorId()));
        } else if (creatorInfo.getCreatorType() == Routine.CreatorType.TEAM) {
            teamRepository.findById(creatorInfo.getCreatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team (creator)", "ID", creatorInfo.getCreatorId()));
        }
        // Ensure acting user is set correctly
        creatorInfo.setActingUserId(actingUser.getUserId());

        if (scheduleType == Routine.ScheduleType.RECURRING && !StringUtils.hasText(recurrenceRule)) {
            throw new ValidationException("Recurrence rule (RRULE) is required for recurring routines.");
        }
        if (startDatetime == null) {
            throw new ValidationException("Start datetime is required.");
        }
        if (!StringUtils.hasText(timezone)) {
            throw new ValidationException("Timezone is required for scheduling.");
        }


        Routine newRoutine = Routine.builder()
                .title(title)
                .descriptivePostId(descriptivePostId)
                .creatorInfo(creatorInfo)
                .scheduleType(scheduleType)
                .startDatetime(startDatetime)
                .endDatetime(endDatetime)
                .recurrenceRule(recurrenceRule)
                .duration(duration)
                .timezone(timezone)
                .purposeOrGoal(purposeOrGoal)
                .locationOrPlatformDetails(locationOrPlatform)
                .participants(initialParticipants != null ? new ArrayList<>(initialParticipants) : new ArrayList<>())
                .visibility(visibility != null ? visibility : Routine.RoutineVisibility.PRIVATE_TO_PARTICIPANTS)
                .status(initialStatus != null ? initialStatus : Routine.RoutineStatus.ACTIVE)
                .linkedProjectId(linkedProjectId)
                .linkedTeamId(linkedTeamId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Calculate initial next occurrence
        newRoutine.setNextOccurrenceDatetime(calculateNextOccurrence(newRoutine.getStartDatetime(), newRoutine.getRecurrenceRule(), newRoutine.getTimezone()));

        logger.info("Creating new routine '{}' by acting user '{}'", newRoutine.getTitle(), actingUser.getUserId());
        return routineRepository.save(newRoutine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Routine> findRoutineById(String routineId) {
        return routineRepository.findById(routineId);
    }

    @Override
    @PreAuthorize("@routineSecurityService.canUpdateRoutine(#routineId, principal.username)")
    public Routine updateRoutineInfo(String routineId,
                                     Map<String, String> title,
                                     String descriptivePostId,
                                     Routine.ScheduleType scheduleType,
                                     LocalDateTime startDatetime,
                                     LocalDateTime endDatetime,
                                     String recurrenceRule,
                                     String duration,
                                     String timezone,
                                     Map<String, String> purposeOrGoal,
                                     Routine.LocationOrPlatformDetails locationOrPlatform,
                                     Routine.RoutineVisibility visibility,
                                     String actingUserId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", "routineId", routineId));

        // checkManagementPermission(routine, actingUserId); // Or use @PreAuthorize

        if (title != null) routine.setTitle(title);
        if (descriptivePostId != null) { // Optional: Validate post existence
            postRepository.findById(descriptivePostId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post (descriptive)", "ID", descriptivePostId));
            routine.setDescriptivePostId(descriptivePostId);
        }
        if (scheduleType != null) routine.setScheduleType(scheduleType);
        if (startDatetime != null) routine.setStartDatetime(startDatetime);
        if (endDatetime != null) routine.setEndDatetime(endDatetime); // Allow setting to null
        if (recurrenceRule != null) routine.setRecurrenceRule(recurrenceRule); // Allow setting to null if not recurring
        if (duration != null) routine.setDuration(duration);
        if (timezone != null) routine.setTimezone(timezone);
        if (purposeOrGoal != null) routine.setPurposeOrGoal(purposeOrGoal);
        if (locationOrPlatform != null) routine.setLocationOrPlatformDetails(locationOrPlatform);
        if (visibility != null) routine.setVisibility(visibility);

        // Recalculate next occurrence if schedule changed
        routine.setNextOccurrenceDatetime(calculateNextOccurrence(routine.getStartDatetime(), routine.getRecurrenceRule(), routine.getTimezone()));
        routine.setUpdatedAt(LocalDateTime.now());
        logger.info("Routine '{}' info updated by user '{}'", routineId, actingUserId);
        return routineRepository.save(routine);
    }

    private void checkManagementPermission(Routine routine, String actingUserId) {
        // Creator or admin of creator team (simplified)
        boolean canManage = false;
        if (routine.getCreatorInfo().getActingUserId().equals(actingUserId)) {
            canManage = true;
        } else if (routine.getCreatorInfo().getCreatorType() == Routine.CreatorType.USER && routine.getCreatorInfo().getCreatorId().equals(actingUserId)) {
            canManage = true;
        } else if (routine.getCreatorInfo().getCreatorType() == Routine.CreatorType.TEAM) {
            Team creatorTeam = teamRepository.findById(routine.getCreatorInfo().getCreatorId()).orElse(null);
            if (creatorTeam != null && creatorTeam.getMembers().stream()
                    .anyMatch(m -> m.getUserId().equals(actingUserId) && m.getRoles().contains("ADMIN"))) {
                canManage = true;
            }
        }
        // Could also check linked project/team managers
        if (!canManage) {
            throw new UnauthorizedException("User " + actingUserId + " is not authorized to manage routine " + routine.getRoutineId());
        }
    }

    @Override
    @PreAuthorize("@routineSecurityService.canChangeRoutineStatus(#routineId, principal.username)")
    public Routine changeRoutineStatus(String routineId, Routine.RoutineStatus newStatus, String actingUserId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", "routineId", routineId));

        // checkManagementPermission(routine, actingUserId);

        routine.setStatus(newStatus);
        if (newStatus == Routine.RoutineStatus.ACTIVE) { // Recalculate next occurrence if reactivated
            routine.setNextOccurrenceDatetime(calculateNextOccurrence(routine.getStartDatetime(), routine.getRecurrenceRule(), routine.getTimezone()));
        } else if (newStatus == Routine.RoutineStatus.COMPLETED || newStatus == Routine.RoutineStatus.CANCELLED) {
            routine.setNextOccurrenceDatetime(null); // No next occurrence
        }
        routine.setUpdatedAt(LocalDateTime.now());
        logger.info("Routine '{}' status changed to {} by user '{}'", routineId, newStatus, actingUserId);
        // notificationService.notifyRoutineParticipants(routineId, "Routine status changed to " + newStatus);
        return routineRepository.save(routine);
    }

    @Override
    @PreAuthorize("@routineSecurityService.canManageRoutineParticipants(#routineId, principal.username)")
    public Routine addParticipantToRoutine(String routineId, Routine.RoutineParticipant participant, String actingUserId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", "routineId", routineId));

        // checkManagementPermission(routine, actingUserId);

        if (participant == null || !StringUtils.hasText(participant.getParticipantId())) {
            throw new ValidationException("Participant details are invalid.");
        }
        // Validate participant existence
        if (participant.getParticipantType() == Routine.ParticipantType.USER) {
            userRepository.findById(participant.getParticipantId())
                    .orElseThrow(() -> new ResourceNotFoundException("User (participant)", "ID", participant.getParticipantId()));
        } else { // TEAM_REPRESENTATIVE
            teamRepository.findById(participant.getParticipantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team (participant)", "ID", participant.getParticipantId()));
        }


        if (routine.getParticipants() == null) {
            routine.setParticipants(new ArrayList<>());
        }
        boolean alreadyParticipant = routine.getParticipants().stream()
                .anyMatch(p -> p.getParticipantId().equals(participant.getParticipantId()) &&
                        p.getParticipantType() == participant.getParticipantType());
        if (alreadyParticipant) {
            throw new ValidationException("Participant already exists in the routine.");
        }

        routine.getParticipants().add(participant);
        routine.setUpdatedAt(LocalDateTime.now());
        logger.info("Participant type '{}' ID '{}' added to routine '{}' by user '{}'",
                participant.getParticipantType(), participant.getParticipantId(), routineId, actingUserId);
        // notificationService.notifyParticipantAddedToRoutine(participant.getParticipantId(), routineId);
        return routineRepository.save(routine);
    }

    @Override
    @PreAuthorize("@routineSecurityService.canManageRoutineParticipants(#routineId, principal.username)")
    public Routine removeParticipantFromRoutine(String routineId, String participantId, Routine.ParticipantType participantType, String actingUserId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", "routineId", routineId));

        // checkManagementPermission(routine, actingUserId);

        if (routine.getParticipants() == null) {
            throw new ResourceNotFoundException("Participant", "ID", participantId + " (type: " + participantType + ")");
        }
        boolean removed = routine.getParticipants().removeIf(p ->
                p.getParticipantId().equals(participantId) && p.getParticipantType() == participantType);

        if (!removed) {
            throw new ResourceNotFoundException("Participant", "ID", participantId + " (type: " + participantType + ")");
        }
        routine.setUpdatedAt(LocalDateTime.now());
        logger.info("Participant type '{}' ID '{}' removed from routine '{}' by user '{}'",
                participantType, participantId, routineId, actingUserId);
        // notificationService.notifyParticipantRemovedFromRoutine(participantId, routineId);
        return routineRepository.save(routine);
    }

    @Override
    public Routine updateParticipantRsvpStatus(String routineId, String participantId, Routine.ParticipantType participantType, Routine.InvitationStatus newStatus, String actingUserId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", "routineId", routineId));

        Routine.RoutineParticipant participant = routine.getParticipants().stream()
                .filter(p -> p.getParticipantId().equals(participantId) && p.getParticipantType() == participantType)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Participant", "ID", participantId + " in routine " + routineId));

        // Check if actingUser is the participant themselves or an organizer/admin
        boolean canUpdateRsvp = actingUserId.equals(participantId) && participantType == Routine.ParticipantType.USER;
        if (!canUpdateRsvp) {
            // Allow if actingUser is creator/admin
            try {
                checkManagementPermission(routine, actingUserId);
                canUpdateRsvp = true;
            } catch (UnauthorizedException e) {
                // Not an admin, and not self-update for USER type
                if(participantType != Routine.ParticipantType.USER || !actingUserId.equals(participantId)) {
                    throw e;
                }
            }
        }
        if (!canUpdateRsvp) {
            throw new UnauthorizedException("User " + actingUserId + " cannot update RSVP for this participant.");
        }

        participant.setInvitationStatus(newStatus);
        routine.setUpdatedAt(LocalDateTime.now());
        logger.info("RSVP status for participant type '{}' ID '{}' in routine '{}' updated to {} by user '{}'",
                participantType, participantId, routineId, newStatus, actingUserId);
        // notificationService.notifyRoutineOrganizers(routineId, "Participant " + participantId + " RSVP changed to " + newStatus);
        return routineRepository.save(routine);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Routine> findRoutinesByParticipantUser(String userId, Pageable pageable) {
        return routineRepository.findByParticipants_ParticipantIdAndParticipants_ParticipantType(userId, Routine.ParticipantType.USER, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Routine> findRoutinesByLinkedProject(String projectId, Pageable pageable) {
        return routineRepository.findByLinkedProjectId(projectId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Routine> findRoutinesByLinkedTeam(String teamId, Pageable pageable) {
        return routineRepository.findByLinkedTeamIdOrCreatorInfo_CreatorIdAndCreatorInfo_CreatorType(
                teamId, teamId, Routine.CreatorType.TEAM, pageable
        ); // Finds if linked or if team is creator
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Routine> findRoutinesByNextOccurrenceBetween(LocalDateTime fromDateTime, LocalDateTime toDateTime, Optional<Routine.RoutineStatus> status, Pageable pageable) {
        if (status.isPresent()) {
            return routineRepository.findByNextOccurrenceDatetimeBetweenAndStatus(fromDateTime, toDateTime, status.get(), pageable);
        } else {
            // Requires a new repository method or more complex query if status is optional for this specific query
            return routineRepository.findByNextOccurrenceDatetimeBetweenAndStatus(fromDateTime, toDateTime, Routine.RoutineStatus.ACTIVE, pageable); // Default to ACTIVE
        }
    }

    @Override
    public Routine recalculateNextOccurrence(String routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", "routineId", routineId));
        if (routine.getStatus() == Routine.RoutineStatus.ACTIVE && routine.getScheduleType() == Routine.ScheduleType.RECURRING) {
            routine.setNextOccurrenceDatetime(calculateNextOccurrence(routine.getStartDatetime(), routine.getRecurrenceRule(), routine.getTimezone()));
            routine.setUpdatedAt(LocalDateTime.now());
            logger.info("Recalculated next occurrence for routine '{}'", routineId);
            return routineRepository.save(routine);
        }
        return routine; // No change if not active or not recurring
    }

    /**
     * Placeholder for calculating the next occurrence.
     * In a real application, use a library like ical4j or similar for RRULE parsing.
     * This method needs to consider the current time to find the *next* valid occurrence.
     * @param firstStartDateTime The initial start datetime of the series.
     * @param rrule The iCalendar RRULE string.
     * @param timezoneStr The timezone string.
     * @return The next occurrence datetime, or null if not applicable/error.
     */
    private LocalDateTime calculateNextOccurrence(LocalDateTime firstStartDateTime, String rrule, String timezoneStr) {
        if (rrule == null || firstStartDateTime == null || timezoneStr == null) {
            return null; // Or return firstStartDateTime if it's a single occurrence and in future
        }
        // --- Placeholder for RRULE parsing logic ---
        // Example:
        // try {
        //     ZoneId zoneId = ZoneId.of(timezoneStr);
        //     RecurrenceRule rule = new RecurrenceRule(rrule); // From a hypothetical library
        //     // DateSeed seed = new DateSeed(java.sql.Timestamp.valueOf(firstStartDateTime.atZone(zoneId).toLocalDateTime()));
        //     // DateTimeIterator iterator = rule.iterator(seed);
        //     // LocalDateTime nowInRoutineTimezone = LocalDateTime.now(zoneId);
        //     // while (iterator.hasNext()) {
        //     //     Date nextDate = iterator.next();
        //     //     LocalDateTime nextOccurrence = LocalDateTime.ofInstant(nextDate.toInstant(), zoneId);
        //     //     if (nextOccurrence.isAfter(nowInRoutineTimezone)) { // Find the first one after now
        //     //         return nextOccurrence;
        //     //     }
        //     // }
        // } catch (Exception e) {
        //     logger.error("Error parsing RRULE '{}' for routine: {}", rrule, e.getMessage());
        //     return null;
        // }
        // For Phase 1, if RRULE is complex, this might just return the start datetime + fixed interval for simplicity
        // or this calculation is deferred / handled by a dedicated scheduler.
        // If it's a simple weekly routine, you could hardcode logic.
        // This is a complex part if full iCalendar support is needed.

        // Simple placeholder: if start is in future, it's the next one. Otherwise, null for now.
        if (firstStartDateTime.isAfter(LocalDateTime.now(ZoneId.of(timezoneStr)))) {
            return firstStartDateTime;
        }
        logger.warn("RRULE parsing and next occurrence calculation is a placeholder for routine with RRULE: {}", rrule);
        return null; // Indicates calculation not fully implemented or no future occurrences found by simple logic
    }
}