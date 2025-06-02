package ir.hamqadam.core.controller.dto.team;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class TeamMemberRoleUpdateRequestDTO {
    @NotEmpty(message = "New roles list cannot be empty")
    private List<String> newRoles;
}