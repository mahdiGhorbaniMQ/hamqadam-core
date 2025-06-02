package ir.hamqadam.core.controller.dto.user;

import ir.hamqadam.core.model.User; // For AccountStatus enum
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserAccountStatusUpdateRequestDTO {
    @NotNull(message = "Account status cannot be null")
    private User.AccountStatus accountStatus;
}