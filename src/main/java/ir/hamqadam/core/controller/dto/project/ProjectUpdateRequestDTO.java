package ir.hamqadam.core.controller.dto.project;

import ir.hamqadam.core.model.Project; // For ProjectVisibility enum
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProjectUpdateRequestDTO {
    @Size(min = 1, message = "Project name cannot be empty if provided")
    private Map<String, String> projectName; // i18n

    private Project.ProjectVisibility visibility;

    private Map<String, String> projectGoals; // i18n
    private Map<String, String> projectScope; // i18n
    private Map<String, String> projectDeliverables; // i18n

    private LocalDateTime startDate;
    private LocalDateTime endDateOrDeadline;

    private String projectPlanLinkOrData;
    private List<Project.ResourceLink> projectResourcesLinks; // Assuming Project.ResourceLink is suitable for DTO
    private List<Project.CommunicationChannel> communicationChannels; // Assuming Project.CommunicationChannel is suitable
}