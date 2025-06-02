package ir.hamqadam.core.controller.dto.project;

import ir.hamqadam.core.model.Project; // For ProjectStatus enum
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectStatusUpdateRequestDTO {
    @NotNull(message = "New project status cannot be null")
    private Project.ProjectStatus newStatus;
}