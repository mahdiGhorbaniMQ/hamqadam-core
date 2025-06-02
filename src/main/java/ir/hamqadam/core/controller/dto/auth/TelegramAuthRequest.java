package ir.hamqadam.core.controller.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

@Data
public class TelegramAuthRequest {
    @NotBlank(message = "Telegram ID cannot be blank")
    private String telegramId;
    private String telegramUsername; // Optional
    private Map<String, String> fullName; // i18n, can be optional if fetched later
    // You might add other fields here if your Telegram auth flow provides more, like a hash for validation
}