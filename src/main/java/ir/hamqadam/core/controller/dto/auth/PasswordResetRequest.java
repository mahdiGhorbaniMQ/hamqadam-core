package ir.hamqadam.core.controller.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;
}