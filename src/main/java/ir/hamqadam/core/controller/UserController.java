package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.common.PageableResponseDTO;
import ir.hamqadam.core.controller.dto.user.NotificationPreferencesUpdateRequestDTO;
import ir.hamqadam.core.controller.dto.user.PrivacySettingsUpdateRequestDTO;
import ir.hamqadam.core.controller.dto.user.UserProfileUpdateRequestDTO;
import ir.hamqadam.core.controller.dto.user.UserResponseDTO;
import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
// Assuming ModelMapper or similar for DTO conversion
// import org.modelmapper.ModelMapper;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    // private final ModelMapper modelMapper; // If using ModelMapper

    @Autowired
    public UserController(UserService userService /*, ModelMapper modelMapper */) {
        this.userService = userService;
        // this.modelMapper = modelMapper;
    }

    /**
     * Gets the profile of the currently authenticated user.
     *
     * @param currentUserDetails Details of the authenticated user.
     * @return ResponseEntity containing the UserResponseDTO.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getCurrentUserProfile(@AuthenticationPrincipal UserDetails currentUserDetails) {
        User user = userService.findUserByEmailOrTelegramId(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        // UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        UserResponseDTO userResponseDTO = convertToUserResponseDTO(user, true); // true for self-view (more details)
        return ResponseEntity.ok(userResponseDTO);
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param currentUserDetails Details of the authenticated user.
     * @param updateRequest DTO containing fields to update.
     * @return ResponseEntity containing the updated UserResponseDTO.
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateCurrentUserProfile(
            @AuthenticationPrincipal UserDetails currentUserDetails,
            @Valid @RequestBody UserProfileUpdateRequestDTO updateRequest) {
        User userToUpdate = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));

        User updatedUser = userService.updateUserProfile(
                userToUpdate.getUserId(),
                updateRequest.getFullName(),
                updateRequest.getBio(),
                updateRequest.getProfilePictures(), // Assuming DTO has compatible structure or service handles it
                updateRequest.getSkills(),
                updateRequest.getPublicContactDetails(),
                updateRequest.getLinkedSocialProfiles()
                // ... pass other updatable fields from DTO
        );
        // UserResponseDTO userResponseDTO = modelMapper.map(updatedUser, UserResponseDTO.class);
        UserResponseDTO userResponseDTO = convertToUserResponseDTO(updatedUser, true);
        return ResponseEntity.ok(userResponseDTO);
    }

    /**
     * Gets the public profile of a user by their ID or handle.
     *
     * @param userIdOrHandle The ID or handle of the user.
     * @return ResponseEntity containing the UserResponseDTO (public view).
     */
    @GetMapping("/{userIdOrHandle}")
    public ResponseEntity<UserResponseDTO> getUserProfile(@PathVariable String userIdOrHandle) {
        // Try finding by ID first, then by handle (if handles are implemented and unique)
        // For simplicity, assuming service handles finding by ID or a unique identifier.
        // If handles are a feature, UserService should have findByHandle.
        User user = userService.findUserById(userIdOrHandle) // Or a method findUserByIdOrHandle
                .orElseThrow(() -> new ResourceNotFoundException("User", "identifier", userIdOrHandle));

        // UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        UserResponseDTO userResponseDTO = convertToUserResponseDTO(user, false); // false for public-view (less details)
        return ResponseEntity.ok(userResponseDTO);
    }

    /**
     * Updates notification preferences for the currently authenticated user.
     * @param currentUserDetails Details of the authenticated user.
     * @param preferencesRequest DTO with notification preferences.
     * @return ResponseEntity with a success message.
     */
    @PutMapping("/me/settings/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> updateNotificationPreferences(
            @AuthenticationPrincipal UserDetails currentUserDetails,
            @Valid @RequestBody NotificationPreferencesUpdateRequestDTO preferencesRequest) {
        User user = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        userService.updateNotificationPreferences(user.getUserId(), preferencesRequest.getPreferences());
        return ResponseEntity.ok(new MessageResponse("Notification preferences updated successfully."));
    }

    /**
     * Updates privacy settings for the currently authenticated user.
     * @param currentUserDetails Details of the authenticated user.
     * @param settingsRequest DTO with privacy settings.
     * @return ResponseEntity with a success message.
     */
    @PutMapping("/me/settings/privacy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> updatePrivacySettings(
            @AuthenticationPrincipal UserDetails currentUserDetails,
            @Valid @RequestBody PrivacySettingsUpdateRequestDTO settingsRequest) {
        User user = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));
        userService.updatePrivacySettings(user.getUserId(), settingsRequest.getSettings());
        return ResponseEntity.ok(new MessageResponse("Privacy settings updated successfully."));
    }

    /**
     * Searches for users based on a query string.
     * Returns publicly searchable users.
     *
     * @param query    The search query string (e.g., name, skill).
     * @param pageable Pagination information.
     * @return A page of UserResponseDTOs.
     */
    @GetMapping("/search")
    public ResponseEntity<PageableResponseDTO<UserResponseDTO>> searchUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "fullName.en") Pageable pageable) { // Default sort by English full name
        Page<User> userPage = userService.searchUsers(query, pageable);
        Page<UserResponseDTO> userResponseDTOPage = userPage.map(user -> convertToUserResponseDTO(user, false)); // Public view

        PageableResponseDTO<UserResponseDTO> response = new PageableResponseDTO<>(
                userResponseDTOPage.getContent(),
                userResponseDTOPage.getNumber(),
                userResponseDTOPage.getSize(),
                userResponseDTOPage.getTotalElements(),
                userResponseDTOPage.getTotalPages(),
                userResponseDTOPage.isLast(),
                userResponseDTOPage.isFirst(),
                userResponseDTOPage.getNumberOfElements(),
                userResponseDTOPage.isEmpty()
        );
        return ResponseEntity.ok(response);
    }


    // --- Helper method for DTO conversion (Placeholder) ---
    // In a real app, use ModelMapper, MapStruct, or dedicated mapper classes.
    private UserResponseDTO convertToUserResponseDTO(User user, boolean isSelfView) {
        if (user == null) return null;
        UserResponseDTO.UserResponseDTOBuilder builder = UserResponseDTO.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName()) // i18n map
                .telegramUsername(user.getTelegramUsername()) // Consider privacy
                .profilePictures(user.getProfilePictures())
                .bio(user.getBio()) // i18n map
                .accountStatus(user.getAccountStatus())
                .createdAt(user.getCreatedAt());

        if (isSelfView) { // More details for the user themselves
            builder.email(user.getEmail())
                    .telegramId(user.getTelegramId())
                    .lastLoginAt(user.getLastLoginAt())
                    .publicContactDetails(user.getPublicContactDetails())
                    .skills(user.getSkills())
                    .interests(user.getInterests())
                    .linkedSocialProfiles(user.getLinkedSocialProfiles())
                    .resumeDetailsText(user.getResumeDetailsText())
                    .resumeFileUrl(user.getResumeFileUrl())
                    .portfolioLinks(user.getPortfolioLinks());
            // Could also add teamMemberships, projectContributions summaries if needed for "me" view
        } else {
            // Public view: Apply privacy settings logic here to determine what is visible
            // For now, just a subset. This part needs careful implementation based on User.privacySettings
            if (/* check privacy for email */ true) { // Replace with actual privacy check
                // builder.email(user.getEmail()); // Example: only show email if public
            }
            if (/* check privacy for skills */ true) {
                builder.skills(user.getSkills());
            }
            // ... and so on for other fields
        }
        return builder.build();
    }
}