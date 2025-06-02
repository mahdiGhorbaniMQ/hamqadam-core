package ir.hamqadam.core.controller.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class NotificationPreferencesUpdateRequestDTO {
    @NotNull(message = "Preferences map cannot be null")
    private Map<String, Object> preferences; // Flexible structure, e.g., {"email_new_comment": true, "telegram_task_assigned": false}
}