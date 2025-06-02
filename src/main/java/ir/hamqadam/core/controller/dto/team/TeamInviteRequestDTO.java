package ir.hamqadam.core.controller.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class TeamInviteRequestDTO {
    @NotBlank(message = "User ID to invite cannot be blank")
    private String userIdToInvite;

    @NotEmpty(message = "At least one role must be specified for the invitation")
    private List<String> roles; // e.g., ["MEMBER"] or ["EDITOR"]
}