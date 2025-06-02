package ir.hamqadam.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String userId; // Can be auto-generated MongoDB ObjectId as String

    // --- Basic Identification ---
    @Field("telegram_id")
    @Indexed(unique = true, sparse = true) // Unique if present, allows nulls
    private String telegramId;

    @Field("telegram_username")
    private String telegramUsername;

    @Field("email")
    @Indexed(unique = true, sparse = true) // Unique if present, allows nulls
    private String email;

    @Field("password_hash")
    private String passwordHash; // Only if email registration with password

    @Field("full_name")
    private Map<String, String> fullName; // i18n: {"en": "John Doe", "fa": "جان دو"}

    // --- Profile Information ---
    @Field("profile_pictures")
    private List<ProfilePicture> profilePictures;

    @Field("bio")
    private Map<String, String> bio; // i18n

    @Field("public_contact_details")
    private Map<String, String> publicContactDetails; // i18n for descriptions, or simple key-value

    @Field("skills")
    private List<String> skills; // Could be List<Map<String, String>> if skills themselves are i18n

    @Field("interests")
    private List<String> interests; // Similar to skills

    @Field("linked_social_profiles")
    private List<SocialProfileLink> linkedSocialProfiles;

    // --- Resume Information ---
    @Field("resume_details_text")
    private Map<String, String> resumeDetailsText; // i18n for textual resume

    @Field("resume_file_url")
    private String resumeFileUrl; // URL to an uploaded resume file

    @Field("portfolio_links")
    private List<String> portfolioLinks;

    // --- Account Status & Auth ---
    @Field("registration_method")
    private RegistrationMethod registrationMethod; // Enum: TELEGRAM, EMAIL

    @Field("is_email_verified")
    private boolean emailVerified;

    @Field("is_telegram_verified")
    private boolean telegramVerified; // Usually true if registered via Telegram

    @Field("account_status")
    private AccountStatus accountStatus; // Enum: ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION

    // --- System Activity Links (Storing IDs for relationships) ---
    @Field("team_memberships")
    private List<TeamMembershipInfo> teamMemberships;

    @Field("project_contributions")
    private List<ProjectContributionInfo> projectContributions;

    @Field("routine_participations")
    private List<RoutineParticipationInfo> routineParticipations;

    @Field("authored_post_ids")
    private List<String> authoredPostIds; // List of Post IDs

    // --- Settings ---
    @Field("notification_preferences")
    private Map<String, Object> notificationPreferences; // Flexible JSON structure

    @Field("privacy_settings")
    private Map<String, Object> privacySettings; // Flexible JSON structure

    // --- Timestamps ---
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("last_login_at")
    private LocalDateTime lastLoginAt;

    // --- Inner classes for embedded documents/structures ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfilePicture {
        private String url;
        private boolean current;
        @Field("uploaded_at")
        private LocalDateTime uploadedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SocialProfileLink {
        @Field("platform_name")
        private String platformName; // e.g., "LinkedIn", "GitHub"
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamMembershipInfo {
        @Field("team_id")
        private String teamId;
        @Field("role_in_team") // For Phase 1, this could be a simple string like "ADMIN" or "MEMBER"
        private String roleInTeam; // Could be List<String> if multiple roles
        @Field("join_date")
        private LocalDateTime joinDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectContributionInfo {
        @Field("project_id")
        private String projectId;
        @Field("role_in_project")
        private String roleInProject; // e.g., "MANAGER", "CONTRIBUTOR"
        @Field("contribution_description")
        private Map<String, String> contributionDescription; // i18n
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoutineParticipationInfo {
        @Field("routine_id")
        private String routineId;
        @Field("role_in_routine")
        private String roleInRoutine; // e.g., "ORGANIZER", "ATTENDEE"
    }

    // --- Enums (Consider defining these in separate files if they grow complex) ---
    public enum RegistrationMethod {
        TELEGRAM, EMAIL
    }

    public enum AccountStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION, DELETED
    }
}