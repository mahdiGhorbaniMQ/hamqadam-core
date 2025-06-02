package ir.hamqadam.core.controller.dto.team;

import ir.hamqadam.core.model.Team.TeamStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamStatusUpdateRequestDTO {
    @NotNull(message = "New team status cannot be null")
    private TeamStatus newStatus;
}