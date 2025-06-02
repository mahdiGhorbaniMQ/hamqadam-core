package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For RoutineStatus enum
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoutineStatusUpdateRequestDTO {
    @NotNull(message = "New routine status cannot be null")
    private Routine.RoutineStatus newStatus;
}