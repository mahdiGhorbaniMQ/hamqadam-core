package ir.hamqadam.core.controller.dto.project;

import ir.hamqadam.core.model.Project; // For ProjectVisibility and ProjectStatus enums
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProjectCreationRequestDTO {
    @NotNull(message = "Project name cannot be null")
    @Size(min = 1, message = "Project name cannot be empty")
    private Map<String, String> projectName; // i18n

    @Size(min = 3, max = 50, message = "Project handle must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Project handle can only contain letters, numbers, underscores, and hyphens")
    private String projectHandle; // Optional

    @NotBlank(message = "Descriptive post ID cannot be blank")
    private String descriptivePostId;

    @NotNull(message = "Creator information cannot be null")
    @Valid // To validate inner fields if ProjectCreatorInfoDTO has constraints
    private ProjectCreatorInfoDTO creatorInfo;

    @NotNull(message = "Visibility cannot be null")
    private Project.ProjectVisibility visibility;

    private Project.ProjectStatus initialStatus; // Optional, defaults in service

    private List<String> managingTeamIds; // Optional list of team IDs

    private LocalDateTime startDate; // Optional
    private LocalDateTime endDateOrDeadline; // Optional

    private Map<String, String> projectGoals; // i18n, Optional
    private Map<String, String> projectScope; // i18n, Optional
}