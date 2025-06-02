package ir.hamqadam.core.controller.dto.user;

import ir.hamqadam.core.model.User; // For ProfilePicture, SocialProfileLink inner classes
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UserProfileUpdateRequestDTO {
    private Map<String, String> fullName; // i18n
    private Map<String, String> bio;      // i18n

    // For profile pictures, the update mechanism might be more complex
    // e.g., separate endpoints for upload/delete/set-current.
    // Or this DTO could define operations: e.g. List<ProfilePictureOperation>
    private List<User.ProfilePicture> profilePictures;

    @Size(max = 50, message = "Too many skills, max 50")
    private List<String> skills;

    @Size(max = 50, message = "Too many interests, max 50")
    private List<String> interests;

    private Map<String, String> publicContactDetails; // i18n
    private List<User.SocialProfileLink> linkedSocialProfiles;
    private Map<String, String> resumeDetailsText; // i18n
    private String resumeFileUrl; // URL might be set after upload via separate endpoint
    private List<String> portfolioLinks;
}