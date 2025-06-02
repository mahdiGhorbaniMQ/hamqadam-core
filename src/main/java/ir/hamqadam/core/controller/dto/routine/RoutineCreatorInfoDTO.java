package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For CreatorType enum
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutineCreatorInfoDTO {
    @NotNull(message = "Creator type cannot be null")
    private Routine.CreatorType creatorType; // USER or TEAM

    @NotBlank(message = "Creator ID cannot be blank")
    private String creatorId; // UserId or TeamId
}