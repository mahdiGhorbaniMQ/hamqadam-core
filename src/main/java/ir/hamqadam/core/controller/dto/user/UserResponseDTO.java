package ir.hamqadam.core.controller.dto.user;

import ir.hamqadam.core.model.User; // For enums and inner classes if needed directly
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private String userId;
    private Map<String, String> fullName; // i18n
    private String email; // May be null depending on privacy
    private String telegramId; // May be null depending on privacy
    private String telegramUsername; // May be null depending on privacy
    private List<User.ProfilePicture> profilePictures; // Assuming User.ProfilePicture is accessible
    private Map<String, String> bio; // i18n
    private User.AccountStatus accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Extended profile fields (visibility controlled by service/privacy settings)
    private Map<String, String> publicContactDetails; // i18n
    private List<String> skills;
    private List<String> interests;
    private List<User.SocialProfileLink> linkedSocialProfiles;
    private Map<String, String> resumeDetailsText; // i18n
    private String resumeFileUrl;
    private List<String> portfolioLinks;

    // Links to other entities (summary or IDs, depending on needs)
    // private List<User.TeamMembershipInfo> teamMemberships;
    // private List<User.ProjectContributionInfo> projectContributions;
    // These might be separate endpoints to get detailed lists for a user
}