package ir.hamqadam.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {

    @Id
    private String notificationId;

    @Indexed
    @Field("recipient_user_id")
    private String recipientUserId; // The user who should receive this notification

    @Field("actor_user_id")
    private String actorUserId; // Optional: The user who performed the action that triggered the notification

    @Field("notification_type")
    private String notificationType; // E.g., "TEAM_INVITATION", "NEW_COMMENT", "TASK_ASSIGNED", "ROUTINE_REMINDER"

    @Field("title")
    private Map<String, String> title; // i18n: e.g., {"en": "New Team Invitation", "fa": "دعوتنامه تیم جدید"}

    @Field("message")
    private Map<String, String> message; // i18n: The main content of the notification

    @Field("related_entity_type")
    private String relatedEntityType; // Optional: e.g., "Team", "Post", "Project", "Task"

    @Field("related_entity_id")
    private String relatedEntityId; // Optional: ID of the related entity

    @Field("is_read")
    @Builder.Default // Lombok builder default
    private boolean read = false;

    @Field("action_url")
    private String actionUrl; // Optional: A URL for the user to click (e.g., to view the team invitation)

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("read_at")
    private LocalDateTime readAt; // When the notification was marked as read
}