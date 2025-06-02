package ir.hamqadam.core.controller.dto.team;

import ir.hamqadam.core.model.Team; // For TeamVisibility enum
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class TeamCreationRequestDTO {
    @NotNull(message = "Team name cannot be null")
    @Size(min = 1, message = "Team name cannot be empty")
    private Map<String, String> teamName; // i18n

    @NotBlank(message = "Team handle cannot be blank")
    @Size(min = 3, max = 30, message = "Team handle must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Team handle can only contain letters, numbers, and underscores")
    private String teamHandle;

    @NotBlank(message = "Introductory post ID cannot be blank")
    private String introductoryPostId;

    private Map<String, String> description; // i18n, optional

    @NotNull(message = "Visibility cannot be null")
    private Team.TeamVisibility visibility;

    private boolean membershipApprovalRequired = true; // Default for public teams
}