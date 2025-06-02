package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For enums and inner classes
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineResponseDTO {
    private String routineId;
    private Map<String, String> title; // i18n
    private String descriptivePostId;
    // private PostSummaryDTO descriptivePost; // Enriched response
    private Routine.CreatorInfo creatorInfo; // Using model's inner class, or a dedicated DTO
    private Routine.RoutineStatus status;
    private Routine.RoutineVisibility visibility;

    // Schedule
    private Routine.ScheduleType scheduleType;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String recurrenceRule;
    private String duration;
    private String timezone;
    private LocalDateTime nextOccurrenceDatetime;

    // Details
    private Map<String, String> purposeOrGoal; // i18n
    private Map<String, String> agendaTemplate; // i18n
    private Routine.LocationOrPlatformDetails locationOrPlatformDetails;
    private String externalLink;

    // Participants
    private List<RoutineParticipantDTO> participants; // Using the enriched DTO

    // Tasks & Reminders
    private List<RoutineTaskOrActionDTO> routineTasksOrActions; // Using the enriched DTO
    private List<Routine.ReminderRule> reminderRules; // Using model's inner class

    // Links
    private String linkedProjectId;
    private String linkedTeamId;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}