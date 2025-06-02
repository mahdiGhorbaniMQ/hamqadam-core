package ir.hamqadam.core.controller.dto.project;

import ir.hamqadam.core.model.Project; // For CreatorType enum
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectCreatorInfoDTO {
    @NotNull(message = "Creator type cannot be null")
    private Project.CreatorType creatorType; // USER or TEAM

    @NotBlank(message = "Creator ID cannot be blank")
    private String creatorId; // UserId or TeamId
}