package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For enums and inner classes
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RoutineCreationRequestDTO {
    @NotNull(message = "Title cannot be null")
    @Size(min = 1, message = "Title cannot be empty")
    private Map<String, String> title; // i18n

    @NotBlank(message = "Descriptive post ID cannot be blank")
    private String descriptivePostId;

    @NotNull(message = "Creator information cannot be null")
    @Valid
    private RoutineCreatorInfoDTO creatorInfo;

    @NotNull(message = "Schedule type cannot be null")
    private Routine.ScheduleType scheduleType;

    @NotNull(message = "Start datetime cannot be null")
    @FutureOrPresent(message = "Start datetime must be now or in the future")
    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime; // Optional

    // Required if scheduleType is RECURRING
    private String recurrenceRule; // iCalendar RRULE string

    @NotBlank(message = "Duration cannot be blank. Use ISO 8601 duration format e.g., PT1H30M.")
    private String duration;

    @NotBlank(message = "Timezone cannot be blank. Use IANA timezone ID e.g., Asia/Tehran.")
    private String timezone;

    private Map<String, String> purposeOrGoal; // i18n, optional

    @Valid
    private Routine.LocationOrPlatformDetails locationOrPlatformDetails; // Optional, using model's inner class

    @Valid
    private List<Routine.RoutineParticipant> initialParticipants; // Optional, using model's inner class for request

    private Routine.RoutineVisibility visibility; // Optional, service defaults
    private Routine.RoutineStatus initialStatus; // Optional, service defaults

    private String linkedProjectId; // Optional
    private String linkedTeamId;  // Optional

    @Valid
    private List<Routine.RoutineTaskOrAction> routineTasksOrActions; // Optional for Phase 1 simplified tasks

    @Valid
    private List<Routine.ReminderRule> reminderRules; // Optional
}