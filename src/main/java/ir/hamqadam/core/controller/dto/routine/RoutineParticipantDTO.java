package ir.hamqadam.core.controller.dto.routine;

import ir.hamqadam.core.model.Routine; // For Enums
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineParticipantDTO { // Can be used for both request (adding) and response
    @NotNull(message = "Participant type cannot be null")
    private Routine.ParticipantType participantType;

    @NotBlank(message = "Participant ID cannot be blank")
    private String participantId;

    private String roleInRoutine; // e.g., "Organizer", "Attendee"

    private Routine.InvitationStatus invitationStatus; // Relevant for response, or when inviting

    private Boolean optional; // Relevant for request when adding/inviting

    // For Response DTO only:
    private String participantName; // Denormalized user/team name (i18n if applicable)
    private String participantProfilePictureUrl;
}