package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For InvitationStatus enum
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoutineRsvpRequestDTO {
    @NotNull(message = "RSVP status cannot be null")
    private Routine.InvitationStatus rsvpStatus;
}