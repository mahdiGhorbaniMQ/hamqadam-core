package ir.hamqadam.core.controller.dto.project;

import ir.hamqadam.core.model.Project; // For enums and inner classes
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
public class ProjectResponseDTO {
    private String projectId;
    private Map<String, String> projectName; // i18n
    private String projectHandle;
    private String descriptivePostId;
    // For creatorInfo, we might want a more detailed DTO if fetching user/team names
    private Project.CreatorInfo creatorInfo; // Using model's inner class for now
    private Project.ProjectStatus status;
    private Project.ProjectVisibility visibility;
    private LocalDateTime startDate;
    private LocalDateTime endDateOrDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Map<String, String> projectGoals; // i18n
    private Map<String, String> projectScope; // i18n
    private Map<String, String> projectDeliverables; // i18n

    private List<String> managingTeamIds; // IDs
    // For display, you might want a list of TeamSummaryDTOs instead of just IDs
    private List<Project.ContributingTeamInfo> contributingTeams; // Using model's inner class
    private List<Project.IndividualContributorInfo> individualContributors; // Using model's inner class

    private String projectPlanLinkOrData;
    private List<Project.ResourceLink> projectResourcesLinks;
    private List<Project.CommunicationChannel> communicationChannels;
    private List<String> linkedRoutineIds;
    private List<String> projectUpdatesPostIds;

    // Potentially add counts or summaries
    // private int taskCount;
    // private int openIssueCount;
}