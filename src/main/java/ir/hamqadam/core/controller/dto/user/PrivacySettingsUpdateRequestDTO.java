package ir.hamqadam.core.controller.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class PrivacySettingsUpdateRequestDTO {
    @NotNull(message = "Settings map cannot be null")
    private Map<String, Object> settings; // Flexible, e.g., {"profile_email_visibility": "TEAM_MEMBERS_ONLY"}
}