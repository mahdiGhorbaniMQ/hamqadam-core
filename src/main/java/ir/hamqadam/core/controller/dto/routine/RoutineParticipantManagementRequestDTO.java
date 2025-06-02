package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For ParticipantType
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoutineParticipantManagementRequestDTO {
    @NotNull(message = "Participant type cannot be null")
    private Routine.ParticipantType participantType;
    @NotBlank(message = "Participant ID cannot be blank")
    private String participantId;
    private String roleInRoutine;
    private Boolean optional; // Default to false if not provided
}