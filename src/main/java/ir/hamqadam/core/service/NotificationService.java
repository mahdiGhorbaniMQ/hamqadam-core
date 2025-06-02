package ir.hamqadam.core.service;

import ir.hamqadam.core.model.Notification;
import ir.hamqadam.core.model.User; // For recipient and actor
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.Post;
import ir.hamqadam.core.model.Comment;
import ir.hamqadam.core.model.Project;
import ir.hamqadam.core.model.Routine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    /**
     * Creates and dispatches a notification.
     * "Dispatching" in Phase 1 might mean saving to DB for in-app, logging,
     * and potentially triggering email/Telegram if configured.
     *
     * @param recipientUserId     ID of the user receiving the notification.
     * @param notificationType    A string key identifying the type of notification.
     * @param titleParams         Parameters for constructing the i18n title.
     * @param messageParams       Parameters for constructing the i18n message.
     * @param relatedEntityType   (Optional) Type of entity related to this notification.
     * @param relatedEntityId     (Optional) ID of the related entity.
     * @param actionUrl           (Optional) URL for a call to action.
     * @param actorUserId         (Optional) ID of the user who initiated the action.
     */
    void sendNotification(String recipientUserId,
                          String notificationType,
                          Map<String, Object> titleParams,
                          Map<String, Object> messageParams,
                          String relatedEntityType,
                          String relatedEntityId,
                          String actionUrl,
                          String actorUserId);

    // --- Specific notification methods (examples) ---

    void notifyTeamInvitation(User invitedUser, Team team, User invitingUser);

    void notifyTeamInvitationResponse(User invitingUser, Team team, User respondingUser, boolean accepted);

    void notifyTeamJoinRequest(List<User> teamAdmins, Team team, User requestingUser);

    void notifyTeamJoinRequestResponse(User requestingUser, Team team, boolean approved);

    void notifyNewPostInTeam(List<User> teamMembersToNotify, Post post, Team team);

    void notifyNewCommentOnPost(User postAuthor, Comment comment, Post post, User commenter);

    void notifyCommentReply(User parentCommentAuthor, Comment reply, Comment parentComment, User replier);

    void notifyUserMentionedInPost(User mentionedUser, Post post, User mentioningUser);

    void notifyUserMentionedInComment(User mentionedUser, Comment comment, User mentioningUser);

    void notifyTaskAssigned(User assignee, /* Task task, */ Project project, User assigner);

    void notifyRoutineReminder(User participant, Routine routine);


    /**
     * Retrieves notifications for a specific user.
     *
     * @param userId   The ID of the user.
     * @param pageable Pagination information.
     * @return A Page of Notifications.
     */
    Page<Notification> getUserNotifications(String userId, Pageable pageable);

    /**
     * Marks a specific notification as read.
     *
     * @param notificationId The ID of the notification.
     * @param userId         The ID of the user who owns the notification (for security).
     * @return True if marked as read, false otherwise.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if notification not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if user does not own notification.
     */
    boolean markNotificationAsRead(String notificationId, String userId);

    /**
     * Marks multiple notifications as read for a user.
     * @param notificationIds List of notification IDs.
     * @param userId The ID of the user.
     * @return Number of notifications marked as read.
     */
    long markMultipleNotificationsAsRead(List<String> notificationIds, String userId);

    /**
     * Marks all unread notifications for a user as read.
     *
     * @param userId The ID of the user.
     * @return The number of notifications marked as read.
     */
    long markAllNotificationsAsRead(String userId);

    /**
     * Gets the count of unread notifications for a user.
     * @param userId The ID of the user.
     * @return Count of unread notifications.
     */
    long getUnreadNotificationCount(String userId);
}