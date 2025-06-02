package ir.hamqadam.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "routines")
public class Routine {

    @Id
    private String routineId;

    // --- Basic Information ---
    @Field("title")
    private Map<String, String> title; // i18n

    @Field("descriptive_post_id") // Mandatory Post describing the routine
    private String descriptivePostId;

    @Field("creator_info")
    private CreatorInfo creatorInfo;

    @Field("status")
    private RoutineStatus status; // Enum: ACTIVE, PAUSED, COMPLETED, ARCHIVED, CANCELLED

    @Field("visibility")
    private RoutineVisibility visibility;

    // --- Scheduling & Recurrence ---
    @Field("schedule_type")
    private ScheduleType scheduleType; // Enum: RECURRING, SINGLE_OCCURRENCE

    @Field("start_datetime")
    private LocalDateTime startDatetime; // Start of first or single occurrence

    @Field("end_datetime")
    private LocalDateTime endDatetime; // Optional: End of last or single occurrence

    @Field("recurrence_rule") // e.g., iCalendar RRULE string for RECURRING type
    private String recurrenceRule;

    @Field("duration") // e.g., "PT1H30M" for 1 hour 30 mins (ISO 8601 duration) or simple minutes
    private String duration;

    @Field("timezone") // e.g., "Asia/Tehran"
    private String timezone;

    @Field("next_occurrence_datetime") // Calculated by system
    private LocalDateTime nextOccurrenceDatetime;

    // --- Content & Execution Details ---
    @Field("purpose_or_goal")
    private Map<String, String> purposeOrGoal; // i18n

    @Field("agenda_template")
    private Map<String, String> agendaTemplate; // i18n

    @Field("location_or_platform_details")
    private LocationOrPlatformDetails locationOrPlatformDetails;

    @Field("external_link")
    private String externalLink; // Optional link to external resource

    // --- Participants & Roles ---
    @Field("participants")
    private List<RoutineParticipant> participants;

    @Field("max_participants")
    private Integer maxParticipants; // Optional

    @Field("rsvp_required")
    private boolean rsvpRequired;

    // --- Tasks/Responsibilities/Action Items within the Routine ---
    @Field("routine_tasks_or_actions")
    private List<RoutineTaskOrAction> routineTasksOrActions;

    // --- Links to Other Entities ---
    @Field("linked_project_id")
    private String linkedProjectId; // Optional

    @Field("linked_team_id")
    private String linkedTeamId; // Optional

    // --- Notification & Reminder Settings ---
    @Field("reminder_rules") // List of rules for sending reminders
    private List<ReminderRule> reminderRules;

    // --- Timestamps ---
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;


    // --- Inner classes ---
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreatorInfo { // Same as in Project
        @Field("creator_type") private CreatorType creatorType;
        @Field("creator_id") private String creatorId;
        @Field("acting_user_id") private String actingUserId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LocationOrPlatformDetails {
        private LocationType type; // Enum: PHYSICAL, ONLINE_LINK, OTHER_PLATFORM
        private Map<String, String> details; // i18n (e.g., room name, or persistent meeting link)
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoutineParticipant {
        @Field("participant_type") private ParticipantType participantType; // Enum: USER, TEAM_REPRESENTATIVE
        @Field("participant_id") private String participantId; // UserId or TeamId
        @Field("role_in_routine") private String roleInRoutine; // e.g., "Organizer", "Attendee"
        @Field("invitation_status") private InvitationStatus invitationStatus; // Enum: INVITED, ACCEPTED, DECLINED
        @Field("is_optional") private boolean optional;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoutineTaskOrAction {
        @Field("task_title") private Map<String, String> taskTitle; // i18n
        @Field("task_description") private Map<String, String> taskDescription; // i18n
        @Field("assigned_to_role_in_routine") private String assignedToRoleInRoutine; // Role name
        @Field("assigned_to_user_id") private String assignedToUserId; // Optional specific user
        @Field("due_condition") private String dueCondition; // e.g., "before_each_occurrence", "after_each_occurrence"
        @Field("tracking_post_id_template") private String trackingPostIdTemplate; // Optional template for updates
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReminderRule {
        private String offset; // e.g., "PT-1H" (1 hour before), "PT-1D" (1 day before) ISO 8601 duration
        @Field("message_template_key") private String messageTemplateKey; // Key for a localized message template
    }

    // --- Enums ---
    public enum CreatorType { USER, TEAM } // Re-declare or put in common place
    public enum RoutineStatus { ACTIVE, PAUSED, COMPLETED, ARCHIVED, CANCELLED }
    public enum RoutineVisibility { PUBLIC, UNLISTED, PRIVATE_TO_PARTICIPANTS, TEAM_ONLY, PROJECT_MEMBERS_ONLY }
    public enum ScheduleType { RECURRING, SINGLE_OCCURRENCE }
    public enum LocationType { PHYSICAL_LOCATION, ONLINE_MEETING_LINK, OTHER_PLATFORM }
    public enum ParticipantType { USER, TEAM_REPRESENTATIVE }
    public enum InvitationStatus { INVITED, ACCEPTED, DECLINED, TENTATIVE }
}