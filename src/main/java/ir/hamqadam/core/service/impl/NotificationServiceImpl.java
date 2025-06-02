package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.exception.UnauthorizedException;
import ir.hamqadam.core.model.*; // Import all models
import ir.hamqadam.core.repository.NotificationRepository;
import ir.hamqadam.core.repository.UserRepository; // To get user details for notifications
// import ir.hamqadam.core.util.I18nMessageConstructor; // A utility to build i18n messages
// import ir.hamqadam.core.integration.EmailService; // If sending emails
// import ir.hamqadam.core.integration.TelegramPushService; // If pushing to Telegram bot

import ir.hamqadam.core.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async; // For asynchronous sending
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // For params
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository; // To fetch user preferences or details
    // private final I18nMessageConstructor messageConstructor; // Hypothetical utility
    // private final EmailService emailService;
    // private final TelegramPushService telegramPushService;

    // Notification Types (constants)
    public static final String TYPE_TEAM_INVITATION = "TEAM_INVITATION";
    public static final String TYPE_TEAM_INVITATION_RESPONSE = "TEAM_INVITATION_RESPONSE";
    public static final String TYPE_TEAM_JOIN_REQUEST = "TEAM_JOIN_REQUEST";
    public static final String TYPE_TEAM_JOIN_REQUEST_RESPONSE = "TEAM_JOIN_REQUEST_RESPONSE";
    public static final String TYPE_NEW_POST_IN_TEAM = "NEW_POST_IN_TEAM";
    public static final String TYPE_NEW_COMMENT_ON_POST = "NEW_COMMENT_ON_POST";
    public static final String TYPE_COMMENT_REPLY = "COMMENT_REPLY";
    public static final String TYPE_USER_MENTIONED_IN_POST = "USER_MENTIONED_IN_POST";
    public static final String TYPE_USER_MENTIONED_IN_COMMENT = "USER_MENTIONED_IN_COMMENT";
    public static final String TYPE_TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TYPE_ROUTINE_REMINDER = "ROUTINE_REMINDER";


    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository
                                   /*, I18nMessageConstructor messageConstructor,
                                   EmailService emailService,
                                   TelegramPushService telegramPushService */) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        // this.messageConstructor = messageConstructor;
        // this.emailService = emailService;
        // this.telegramPushService = telegramPushService;
    }

    @Override
    @Async // Make the notification sending process asynchronous
    public void sendNotification(String recipientUserId,
                                 String notificationType,
                                 Map<String, Object> titleParams,
                                 Map<String, Object> messageParams,
                                 String relatedEntityType,
                                 String relatedEntityId,
                                 String actionUrl,
                                 String actorUserId) {

        User recipient = userRepository.findById(recipientUserId)
                .orElse(null); // Or handle more gracefully

        if (recipient == null) {
            logger.warn("Recipient user with ID {} not found. Notification not sent.", recipientUserId);
            return;
        }

        // TODO: Check user's notification preferences for this type and channel
        // boolean prefersInApp = recipient.getNotificationPreferences().getOrDefault("in_app_" + notificationType, true);
        // boolean prefersEmail = recipient.getNotificationPreferences().getOrDefault("email_" + notificationType, false);
        // boolean prefersTelegram = recipient.getNotificationPreferences().getOrDefault("telegram_" + notificationType, false);

        // For Phase 1, let's focus on creating an in-app notification record.
        // Construct i18n title and message (placeholder - use a real template engine or I18nMessageConstructor)
        Map<String, String> title = constructI18nText("notification." + notificationType + ".title", titleParams, recipient);
        Map<String, String> message = constructI18nText("notification." + notificationType + ".message", messageParams, recipient);

        Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .actorUserId(actorUserId)
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .actionUrl(actionUrl)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        logger.info("In-app notification '{}' created for user '{}'", notificationType, recipientUserId);

        // if (prefersEmail) {
        //     // emailService.sendNotificationEmail(recipient, title.get("en"), message.get("en"), actionUrl);
        // }
        // if (prefersTelegram && recipient.getTelegramId() != null) {
        //     // telegramPushService.sendTelegramNotification(recipient.getTelegramId(), message.get("fa"), actionUrl); // Assuming Persian for Telegram
        // }
    }

    // Placeholder for i18n message construction
    private Map<String, String> constructI18nText(String messageKey, Map<String, Object> params, User recipient) {
        // In a real app, this would use Spring's MessageSource or a template engine
        // to resolve messageKey with params for different locales.
        // For now, a very simple placeholder.
        Map<String, String> i18nText = new HashMap<>();
        String englishText = messageKey + (params != null ? " " + params.toString() : "");
        String persianText = messageKey + "_fa" + (params != null ? " " + params.toString() : ""); // Example

        // Example: "User {actorName} invited you to team {teamName}"
        if (params != null) {
            if (params.containsKey("actorName")) {
                englishText = englishText.replace("{actorName}", params.get("actorName").toString());
                persianText = persianText.replace("{actorName}", params.get("actorName").toString());
            }
            if (params.containsKey("teamName")) {
                englishText = englishText.replace("{teamName}", params.get("teamName").toString());
                persianText = persianText.replace("{teamName}", params.get("teamName").toString());
            }
            // Add more parameter replacements as needed
        }

        i18nText.put("en", englishText);
        i18nText.put("fa", persianText); // Assuming 'fa' for Persian
        return i18nText;
    }


    // --- Implementations for specific notification methods ---
    // These methods would call the generic sendNotification method with appropriate parameters.

    @Override
    public void notifyTeamInvitation(User invitedUser, Team team, User invitingUser) {
        Map<String, Object> titleParams = Map.of("teamName", team.getTeamName().getOrDefault("en", "a team")); // Default lang
        Map<String, Object> messageParams = Map.of(
                "invitingUserName", invitingUser.getFullName().getOrDefault("en", "Someone"),
                "teamName", team.getTeamName().getOrDefault("en", "a team")
        );
        String actionUrl = "/teams/" + team.getTeamId() + "/invitations"; // Example URL

        sendNotification(invitedUser.getUserId(), TYPE_TEAM_INVITATION, titleParams, messageParams,
                "Team", team.getTeamId(), actionUrl, invitingUser.getUserId());
    }

    @Override
    public void notifyTeamInvitationResponse(User invitingUser, Team team, User respondingUser, boolean accepted) {
        String response = accepted ? "accepted" : "declined";
        Map<String, Object> titleParams = Map.of("userName", respondingUser.getFullName().getOrDefault("en", "Someone"));
        Map<String, Object> messageParams = Map.of(
                "userName", respondingUser.getFullName().getOrDefault("en", "Someone"),
                "teamName", team.getTeamName().getOrDefault("en", "a team"),
                "response", response
        );
        String actionUrl = "/teams/" + team.getTeamId() + "/members";

        sendNotification(invitingUser.getUserId(), TYPE_TEAM_INVITATION_RESPONSE, titleParams, messageParams,
                "Team", team.getTeamId(), actionUrl, respondingUser.getUserId());
    }

    @Override
    public void notifyTeamJoinRequest(List<User> teamAdmins, Team team, User requestingUser) {
        Map<String, Object> titleParams = Map.of("userName", requestingUser.getFullName().getOrDefault("en", "Someone"));
        Map<String, Object> messageParams = Map.of(
                "userName", requestingUser.getFullName().getOrDefault("en", "Someone"),
                "teamName", team.getTeamName().getOrDefault("en", "a team")
        );
        String actionUrl = "/teams/" + team.getTeamId() + "/requests";
        for (User admin : teamAdmins) {
            sendNotification(admin.getUserId(), TYPE_TEAM_JOIN_REQUEST, titleParams, messageParams,
                    "Team", team.getTeamId(), actionUrl, requestingUser.getUserId());
        }
    }

    @Override
    public void notifyTeamJoinRequestResponse(User requestingUser, Team team, boolean approved) {
        String responseStatus = approved ? "approved" : "declined";
        Map<String, Object> titleParams = Map.of("teamName", team.getTeamName().getOrDefault("en", "A team"));
        Map<String, Object> messageParams = Map.of(
                "teamName", team.getTeamName().getOrDefault("en", "a team"),
                "status", responseStatus
        );
        String actionUrl = "/teams/" + team.getTeamId();

        sendNotification(requestingUser.getUserId(), TYPE_TEAM_JOIN_REQUEST_RESPONSE, titleParams, messageParams,
                "Team", team.getTeamId(), actionUrl, null); // System action or team admin action
    }

    // Implement other specific notification methods (notifyNewPostInTeam, notifyNewCommentOnPost, etc.)
    // similarly, by preparing params and calling sendNotification.

    @Override
    public void notifyNewCommentOnPost(User postAuthor, Comment comment, Post post, User commenter) {
        if(postAuthor.getUserId().equals(commenter.getUserId())) return; // Don't notify for own comment

        Map<String, Object> titleParams = Map.of("postTitle", post.getTitle().getOrDefault("en", "your post"));
        Map<String, Object> messageParams = Map.of(
                "commenterName", commenter.getFullName().getOrDefault("en", "Someone"),
                "postTitle", post.getTitle().getOrDefault("en", "your post")
        );
        String actionUrl = "/posts/" + post.getPostId() + "#comment-" + comment.getCommentId();
        sendNotification(postAuthor.getUserId(), TYPE_NEW_COMMENT_ON_POST, titleParams, messageParams,
                "Post", post.getPostId(), actionUrl, commenter.getUserId());
    }

    @Override
    public void notifyCommentReply(User parentCommentAuthor, Comment reply, Comment parentComment, User replier) {
        if(parentCommentAuthor.getUserId().equals(replier.getUserId())) return; // Don't notify for own reply

        Map<String, Object> titleParams = Map.of("replierName", replier.getFullName().getOrDefault("en", "Someone"));
        Map<String, Object> messageParams = Map.of(
                "replierName", replier.getFullName().getOrDefault("en", "Someone")
        );
        // Action URL should ideally point to the specific reply or the parent comment
        String actionUrl = "/posts/" + parentComment.getTargetEntityId() + "#comment-" + reply.getCommentId();
        if("Post".equalsIgnoreCase(parentComment.getTargetEntityType())){ // Check target entity type
            actionUrl = "/posts/" + parentComment.getTargetEntityId() + "#comment-" + reply.getCommentId();
        } else {
            // Construct URL based on other entity types if applicable
        }


        sendNotification(parentCommentAuthor.getUserId(), TYPE_COMMENT_REPLY, titleParams, messageParams,
                parentComment.getTargetEntityType(), parentComment.getTargetEntityId(), actionUrl, replier.getUserId());
    }

    // Stubs for other notification methods, to be implemented fully:
    @Override public void notifyNewPostInTeam(List<User> teamMembersToNotify, Post post, Team team) { /* ... */ }
    @Override public void notifyUserMentionedInPost(User mentionedUser, Post post, User mentioningUser) { /* ... */ }
    @Override public void notifyUserMentionedInComment(User mentionedUser, Comment comment, User mentioningUser) { /* ... */ }
    @Override public void notifyTaskAssigned(User assignee, /* Task task, */ Project project, User assigner) { /* ... */ }
    @Override public void notifyRoutineReminder(User participant, Routine routine) { /* ... */ }


    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public boolean markNotificationAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "ID", notificationId));

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new UnauthorizedException("User not authorized to mark this notification as read.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            return true;
        }
        return false;
    }

    @Override
    public long markMultipleNotificationsAsRead(List<String> notificationIds, String userId) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        List<Notification> userNotificationsToUpdate = notifications.stream()
                .filter(n -> n.getRecipientUserId().equals(userId) && !n.isRead())
                .peek(n -> {
                    n.setRead(true);
                    n.setReadAt(LocalDateTime.now());
                })
                .collect(Collectors.toList());

        if (!userNotificationsToUpdate.isEmpty()) {
            notificationRepository.saveAll(userNotificationsToUpdate);
        }
        return userNotificationsToUpdate.size();
    }

    @Override
    public long markAllNotificationsAsRead(String userId) {
        // This could be inefficient if there are many unread notifications.
        // A custom repository update query might be better for performance.
        Page<Notification> unreadNotificationsPage = notificationRepository.findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(userId, Pageable.unpaged());
        List<Notification> unreadNotifications = unreadNotificationsPage.getContent();

        if (!unreadNotifications.isEmpty()) {
            for (Notification notification : unreadNotifications) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
            }
            notificationRepository.saveAll(unreadNotifications);
        }
        return unreadNotifications.size();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }
}