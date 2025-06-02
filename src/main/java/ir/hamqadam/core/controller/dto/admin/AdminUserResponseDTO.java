package ir.hamqadam.core.controller.dto.admin;

import ir.hamqadam.core.model.User; // For enums and inner classes
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDTO {
    private String userId;
    private Map<String, String> fullName; // i18n
    private String email;
    private String telegramId;
    private String telegramUsername;
    private List<User.ProfilePicture> profilePictures;
    private Map<String, String> bio; // i18n

    private User.RegistrationMethod registrationMethod;
    private boolean emailVerified;
    private boolean telegramVerified;
    private User.AccountStatus accountStatus;

    private List<User.TeamMembershipInfo> teamMemberships; // Could be just IDs or summary
    // Consider adding roles from a central RBAC system if implemented beyond simple strings

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // Other admin-specific fields if needed
    // e.g., count of posts, projects, etc.
}