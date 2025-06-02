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
@Document(collection = "projects")
public class Project {

    @Id
    private String projectId;

    // --- Basic Information ---
    @Field("project_name")
    private Map<String, String> projectName; // i18n

    @Field("project_handle")
    @Indexed(unique = true, sparse = true) // Unique if provided
    private String projectHandle;

    @Field("descriptive_post_id")
    private String descriptivePostId; // ID of the Post describing this project

    @Field("creator_info")
    private CreatorInfo creatorInfo;

    @Field("status")
    private ProjectStatus status; // Phase 1: Enum for fixed workflow stages

    @Field("visibility")
    private ProjectVisibility visibility; // Enum: PUBLIC, PRIVATE_TO_MEMBERS

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("end_date_or_deadline")
    private LocalDateTime endDateOrDeadline;

    // --- Teams & Contributors ---
    @Field("managing_team_ids") // Teams primarily responsible
    private List<String> managingTeamIds;

    @Field("contributing_teams") // Other teams involved
    private List<ContributingTeamInfo> contributingTeams;

    @Field("individual_contributors")
    private List<IndividualContributorInfo> individualContributors;

    // --- Project Content & Components (mostly links or references for Phase 1) ---
    @Field("project_goals")
    private Map<String, String> projectGoals; // i18n

    @Field("project_scope")
    private Map<String, String> projectScope; // i18n

    @Field("project_deliverables")
    private Map<String, String> projectDeliverables; // i18n

    // For Phase 1, actual task/issue management might be simplified or external.
    // This could be a link to an external board or a very simple embedded structure.
    @Field("tasks_reference_info")
    private String tasksReferenceInfo; // e.g., "See Trello board: <link>" or simple task list ID

    @Field("project_plan_link_or_data")
    private String projectPlanLinkOrData; // URL or reference

    @Field("project_resources_links")
    private List<ResourceLink> projectResourcesLinks;

    @Field("linked_routine_ids")
    private List<String> linkedRoutineIds; // Routines specific to this project

    @Field("project_updates_post_ids")
    private List<String> projectUpdatesPostIds; // Posts that are updates for this project

    // --- Settings ---
    @Field("communication_channels")
    private List<CommunicationChannel> communicationChannels;

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
    public static class CreatorInfo {
        @Field("creator_type")
        private CreatorType creatorType; // Enum: USER, TEAM
        @Field("creator_id")
        private String creatorId; // UserId or TeamId
        @Field("acting_user_id")
        private String actingUserId; // UserId who performed the action
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContributingTeamInfo {
        @Field("team_id")
        private String teamId;
        @Field("role_in_project")
        private String roleInProject; // e.g., "Development", "Marketing"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndividualContributorInfo {
        @Field("user_id")
        private String userId;
        @Field("role_in_project")
        private String roleInProject;
        @Field("contribution_description")
        private Map<String, String> contributionDescription; // i18n
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResourceLink {
        private Map<String, String> title; // i18n
        private String url;
        private Map<String, String> description; // i18n
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommunicationChannel {
        private Map<String, String> name; // i18n e.g., "Telegram Group", "Slack Channel"
        private String link;
    }

    // --- Enums ---
    public enum CreatorType {
        USER, TEAM
    }

    public enum ProjectStatus { // Phase 1 Fixed Workflow Stages
        IDEA_PROPOSAL, PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, ARCHIVED, CANCELLED
    }

    public enum ProjectVisibility {
        PUBLIC,
        PRIVATE_TO_MEMBERS // Only members of associated teams/contributors
    }
}