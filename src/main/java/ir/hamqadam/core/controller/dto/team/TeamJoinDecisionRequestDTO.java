package ir.hamqadam.core.controller.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamJoinDecisionRequestDTO {
    @NotNull(message = "Decision (accept/approve or reject/decline) cannot be null")
    private Boolean accept; // true for accept/approve, false for reject/decline
}