package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.admin.AdminUserResponseDTO;
import ir.hamqadam.core.controller.dto.admin.SystemSettingDTO;
import ir.hamqadam.core.controller.dto.admin.SystemSettingsUpdateRequestDTO;
import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.common.PageableResponseDTO;
import ir.hamqadam.core.controller.dto.user.UserAccountStatusUpdateRequestDTO; // Reusing
import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.UserService;
// import ir.hamqadam.core.service.SystemSettingsService; // If you create a dedicated service

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
// import org.modelmapper.ModelMapper;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')") // Secure all endpoints in this controller
public class AdminController {

    private final UserService userService;
    // private final SystemSettingsService systemSettingsService; // Optional dedicated service
    // private final ModelMapper modelMapper;

    @Autowired
    public AdminController(UserService userService /*, SystemSettingsService systemSettingsService, ModelMapper modelMapper */) {
        this.userService = userService;
        // this.systemSettingsService = systemSettingsService;
        // this.modelMapper = modelMapper;
    }

    /**
     * Lists all users with pagination for administrative purposes.
     *
     * @param pageable Pagination information.
     * @return A page of AdminUserResponseDTOs.
     */
    @GetMapping("/users")
    public ResponseEntity<PageableResponseDTO<AdminUserResponseDTO>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<User> userPage = userService.findAllUsers(pageable); // Assuming UserService has this admin method
        Page<AdminUserResponseDTO> dtoPage = userPage.map(this::convertToAdminUserResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    /**
     * Gets detailed information for a specific user by ID for admin.
     * @param userId The ID of the user.
     * @return ResponseEntity with AdminUserResponseDTO.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponseDTO> getUserByIdForAdmin(@PathVariable String userId) {
        User user = userService.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        return ResponseEntity.ok(convertToAdminUserResponseDTO(user));
    }

    /**
     * Updates the account status of a specific user.
     *
     * @param userId        The ID of the user whose status is to be updated.
     * @param statusRequest DTO containing the new account status.
     * @return ResponseEntity containing the updated AdminUserResponseDTO.
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponseDTO> updateUserAccountStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserAccountStatusUpdateRequestDTO statusRequest) {
        User updatedUser = userService.updateUserAccountStatus(userId, statusRequest.getAccountStatus());
        return ResponseEntity.ok(convertToAdminUserResponseDTO(updatedUser));
    }

    /**
     * Retrieves current system settings. (Phase 1: Simplified)
     * This would interact with a SystemSettingsService or a properties-based configuration manager.
     *
     * @return A list of system settings.
     */
    @GetMapping("/settings")
    public ResponseEntity<List<SystemSettingDTO>> getSystemSettings() {
        // Placeholder: In a real app, fetch these from a service that reads from DB or config file.
        // List<SystemSettingDTO> settings = systemSettingsService.getAllSettings();
        List<SystemSettingDTO> settings = List.of(
                new SystemSettingDTO("site_name", "hamqadam Platform", new HashMap<>()),
                new SystemSettingDTO("max_team_members", "50", new HashMap<>()),
                new SystemSettingDTO("allow_new_registrations", "true", new HashMap<>())
        ); // Example data
        return ResponseEntity.ok(settings);
    }

    /**
     * Updates system settings. (Phase 1: Simplified)
     *
     * @param settingsRequest DTO containing a list of settings to update.
     * @return ResponseEntity with a success message.
     */
    @PutMapping("/settings")
    public ResponseEntity<MessageResponse> updateSystemSettings(
            @Valid @RequestBody SystemSettingsUpdateRequestDTO settingsRequest) {
        // Placeholder: In a real app, a SystemSettingsService would handle updating these.
        // systemSettingsService.updateSettings(settingsRequest.getSettings());
        settingsRequest.getSettings().forEach(setting ->
                System.out.println("Admin updating setting: " + setting.getKey() + " = " + setting.getValue())
        );
        return ResponseEntity.ok(new MessageResponse("System settings updated successfully."));
    }

    // --- Helper method for DTO conversion (Placeholder) ---
    private AdminUserResponseDTO convertToAdminUserResponseDTO(User user) {
        if (user == null) return null;
        // Use ModelMapper or MapStruct in a real application
        return AdminUserResponseDTO.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .telegramId(user.getTelegramId())
                .telegramUsername(user.getTelegramUsername())
                .profilePictures(user.getProfilePictures())
                .bio(user.getBio())
                .registrationMethod(user.getRegistrationMethod())
                .emailVerified(user.isEmailVerified())
                .telegramVerified(user.isTelegramVerified())
                .accountStatus(user.getAccountStatus())
                .teamMemberships(user.getTeamMemberships()) // For admin view, could show more detail
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}