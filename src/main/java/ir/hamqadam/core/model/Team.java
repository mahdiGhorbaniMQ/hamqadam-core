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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "teams")
public class Team {

    @Id
    private String teamId;

    // --- Basic Information ---
    @Field("team_name")
    private Map<String, String> teamName; // i18n

    @Field("team_handle")
    @Indexed(unique = true)
    private String teamHandle; // Unique, short identifier (e.g., @team_alpha)

    @Field("creator_user_id")
    private String creatorUserId; // User ID of the team founder

    @Field("introductory_post_id")
    private String introductoryPostId; // ID of the Post that introduces this team

    @Field("description")
    private Map<String, String> description; // i18n, can be a summary or distinct from intro post

    @Field("profile_picture_url")
    private String profilePictureUrl;

    @Field("cover_picture_url")
    private String coverPictureUrl;

    // --- Membership and Roles ---
    @Field("members")
    private List<TeamMember> members;

    // For Phase 1, defined_team_roles might be simple strings used in TeamMember.roles.
    // For more advanced RBAC, this could link to a global Role entity with a team scope.
    @Field("defined_team_roles") // e.g., ["Lead Developer", "Designer", "QA"] specific to this team context
    private List<Map<String, String>> definedTeamRoles; // Each map is i18n for role name/description

    // --- Team Content & Associations ---
    @Field("linked_telegram_group_id")
    private String linkedTelegramGroupId; // Optional

    @Field("associated_project_ids")
    private List<String> associatedProjectIds; // List of Project IDs

    @Field("associated_routine_ids")
    private List<String> associatedRoutineIds; // List of Routine IDs

    @Field("authored_content_ids")
    private List<String> authoredContentIds; // List of Post IDs authored by this team

    @Field("team_portfolio_links")
    private List<String> teamPortfolioLinks;

    // --- Structure & Relations ---
    @Field("parent_team_id")
    private String parentTeamId; // If this is a sub-team

    @Field("child_team_ids")
    private List<String> childTeamIds; // List of sub-team IDs

    @Field("collaboration_agreements_ids") // Could point to a separate collection or be embedded if simple
    private List<String> collaborationAgreementIds; // IDs of agreements with other teams

    // --- Settings ---
    @Field("visibility")
    private TeamVisibility visibility; // Enum: PUBLIC, PRIVATE

    @Field("membership_approval_required")
    private boolean membershipApprovalRequired; // For public teams

    @Field("team_status")
    private TeamStatus teamStatus; // Enum: ACTIVE, ARCHIVED, DISBANDED

    // --- Timestamps ---
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    // --- Inner classes ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamMember {
        @Field("user_id")
        private String userId;
        private List<String> roles; // Simple role names for Phase 1 (e.g., "ADMIN", "EDITOR", "MEMBER")
        @Field("join_date")
        private LocalDateTime joinDate;
        @Field("status_in_team")
        private MemberStatus statusInTeam; // Enum: ACTIVE, PENDING_APPROVAL, INVITED
    }

    // --- Enums ---
    public enum TeamVisibility {
        PUBLIC, PRIVATE
    }

    public enum TeamStatus {
        ACTIVE, ARCHIVED, DISBANDED
    }

    public enum MemberStatus {
        ACTIVE, PENDING_APPROVAL, INVITED
    }
}