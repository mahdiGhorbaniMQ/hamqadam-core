package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For enums and inner classes
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RoutineUpdateRequestDTO {
    @Size(min = 1, message = "Title cannot be empty if provided")
    private Map<String, String> title; // i18n

    private String descriptivePostId; // If updatable

    // Schedule fields - all optional, service handles partial updates
    private Routine.ScheduleType scheduleType;
    @FutureOrPresent(message = "Start datetime must be now or in the future if provided")
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String recurrenceRule;
    private String duration;
    private String timezone;

    private Map<String, String> purposeOrGoal; // i18n
    @Valid
    private Routine.LocationOrPlatformDetails locationOrPlatformDetails;
    private Routine.RoutineVisibility visibility;

    // Participants and tasks might be managed via separate endpoints for more granular control
    // Or can be part of this DTO if replacing the whole list is intended.
    // For now, assume separate endpoints are better for managing collections.

    @Valid
    private List<Routine.RoutineTaskOrAction> routineTasksOrActions; // For updating the whole list

    @Valid
    private List<Routine.ReminderRule> reminderRules;
}