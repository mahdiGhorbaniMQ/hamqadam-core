package ir.hamqadam.core.controller.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class UserRegistrationRequest {
    @NotEmpty(message = "Full name cannot be empty")
    private Map<String, String> fullName; // i18n: {"en": "John Doe", "fa": "جان دو"}

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}