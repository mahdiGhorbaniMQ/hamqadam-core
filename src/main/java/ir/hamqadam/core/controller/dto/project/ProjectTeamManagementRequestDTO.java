package ir.hamqadam.core.controller.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectTeamManagementRequestDTO {
    @NotBlank(message = "Team ID cannot be blank")
    private String teamId;
    private boolean isManagingTeam; // True if adding as managing, false if as contributing
    private String roleInProject; // Optional, relevant for contributing teams primarily
}