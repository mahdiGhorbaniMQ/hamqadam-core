package ir.hamqadam.core.service;

import ir.hamqadam.core.model.User;
import ir.hamqadam.core.model.User.AccountStatus; // Assuming AccountStatus is an inner enum or separate
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService; // Spring Security's UserDetailsService

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService  { // Extending UserDetailsService for Spring Security

    /**
     * Registers a new user with email and password.
     *
     * @param fullName i18n map of full name
     * @param email    User's email
     * @param password Raw password
     * @return The created User object
     * @throws ir.hamqadam.core.exception.ValidationException if email already exists or data is invalid
     */
    User registerNewUserByEmail(Map<String, String> fullName, String email, String password);

    /**
     * Registers or logs in a user via Telegram.
     * If user exists, updates last login. If not, creates a new user.
     *
     * @param telegramId       Telegram User ID
     * @param telegramUsername Telegram Username (can be null)
     * @param fullNameFromTelegram i18n map of full name obtained from Telegram
     * @return The created or fetched User object
     */
    User registerOrLoginTelegramUser(String telegramId, String telegramUsername, Map<String, String> fullNameFromTelegram);

    /**
     * Finds a user by their ID.
     *
     * @param userId The ID of the user.
     * @return An Optional containing the user if found.
     */
    Optional<User> findUserById(String userId);

    /**
     * Finds a user by their email.
     *
     * @param email The email of the user.
     * @return An Optional containing the user if found.
     */
    Optional<User> findUserByEmail(String email);

    /**
     * Finds a user by their Telegram ID.
     *
     * @param telegramId The Telegram ID of the user.
     * @return An Optional containing the user if found.
     */
    Optional<User> findUserByTelegramId(String telegramId);

    /**
     * Updates a user's profile information.
     *
     * @param userId            The ID of the user to update.
     * @param updatedUserFields A User object or DTO containing only the fields to be updated.
     * For this example, let's assume it's a map or a specific DTO.
     * For simplicity, we can pass individual updatable fields.
     * @return The updated User object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if user not found.
     */
    User updateUserProfile(String userId,
                           Map<String, String> fullName, // i18n
                           Map<String, String> bio,      // i18n
                           List<User.ProfilePicture> profilePictures, // Assuming ProfilePicture structure
                           List<String> skills,
                           // ... other updatable fields like resumeDetailsText, resumeFileUrl etc.
                           Map<String, String> publicContactDetails,
                           List<User.SocialProfileLink> linkedSocialProfiles
    );

    /**
     * Updates user's notification preferences.
     * @param userId User ID
     * @param notificationPreferences Map representing preferences
     * @return Updated User
     */
    User updateNotificationPreferences(String userId, Map<String, Object> notificationPreferences);

    /**
     * Updates user's privacy settings.
     * @param userId User ID
     * @param privacySettings Map representing settings
     * @return Updated User
     */
    User updatePrivacySettings(String userId, Map<String, Object> privacySettings);

    /**
     * Changes a user's password.
     * @param userId User ID
     * @param oldPassword Current raw password
     * @param newPassword New raw password
     * @return boolean indicating success
     * @throws ir.hamqadam.core.exception.ValidationException if old password doesn't match or new password invalid
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if user not found
     */
    boolean changePassword(String userId, String oldPassword, String newPassword);

    /**
     * Verifies a user's email using a verification token.
     * @param token The verification token.
     * @return boolean indicating success.
     * @throws ir.hamqadam.core.exception.ValidationException if token is invalid or expired.
     */
    // boolean verifyEmail(String token); // Logic for token generation/storage needed

    /**
     * (Admin) Updates a user's account status.
     * @param userId The ID of the user.
     * @param newStatus The new account status.
     * @return The updated User object.
     */
    User updateUserAccountStatus(String userId, AccountStatus newStatus);

    /**
     * (Admin) Finds all users with pagination.
     * @param pageable Pagination information.
     * @return A Page of Users.
     */
    Page<User> findAllUsers(Pageable pageable);

    /**
     * Searches for users based on a query string (e.g., name, email, skill).
     * For Phase 1, this might be a simple query.
     * @param query The search query.
     * @param pageable Pagination information.
     * @return A Page of matching Users.
     */
    Page<User> searchUsers(String query, Pageable pageable);
}