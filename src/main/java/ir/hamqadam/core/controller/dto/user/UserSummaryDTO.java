package ir.hamqadam.core.controller.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private String userId;
    private Map<String, String> fullName; // i18n
    private String profilePictureUrl;
    // Add other minimal fields if needed, e.g., handle
}