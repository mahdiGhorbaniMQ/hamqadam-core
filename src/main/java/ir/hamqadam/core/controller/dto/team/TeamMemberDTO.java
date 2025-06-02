package ir.hamqadam.core.controller.dto.team;

import ir.hamqadam.core.model.Team; // For MemberStatus enum
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDTO {
    private String userId;
    private String userFullName; // Denormalized for display, consider i18n if User's fullName is i18n
    private String userProfilePictureUrl; // Denormalized
    private List<String> roles; // e.g., ["ADMIN", "MEMBER"]
    private LocalDateTime joinDate;
    private Team.MemberStatus statusInTeam;
}