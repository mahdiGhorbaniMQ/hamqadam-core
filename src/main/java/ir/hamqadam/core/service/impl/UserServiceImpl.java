package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.model.User.AccountStatus;
import ir.hamqadam.core.repository.UserRepository;
import ir.hamqadam.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional // Add transactional behavior to service methods
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Implementation of UserDetailsService ---
//    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UsernameNotFoundException("User account is not active: " + email);
        }

        // For Phase 1, let's assign a default "ROLE_USER".
        // This should be expanded with actual role management from User entity if available.
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        // if(user.isAdmin()) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")); // Example

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                true, true, true, true, // enabled, accountNonExpired, credentialsNonExpired, accountNonLocked
                authorities
        );
    }

    // --- UserService Methods ---

    @Override
    public User registerNewUserByEmail(Map<String, String> fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email address already in use: " + email);
        }
        if (!StringUtils.hasText(password) || password.length() < 8) { // Example validation
            throw new ValidationException("Password must be at least 8 characters long.");
        }

        User newUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .registrationMethod(User.RegistrationMethod.EMAIL)
                .accountStatus(AccountStatus.PENDING_VERIFICATION) // Or ACTIVE if no email verification
                .emailVerified(false)
                .telegramVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Here you would typically trigger an email verification process
        logger.info("Registering new user by email: {}", email);
        return userRepository.save(newUser);
    }

    @Override
    public User registerOrLoginTelegramUser(String telegramId, String telegramUsername, Map<String, String> fullNameFromTelegram) {
        Optional<User> existingUserOpt = userRepository.findByTelegramId(telegramId);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            existingUser.setLastLoginAt(LocalDateTime.now());
            // Optionally update username or full name if they've changed in Telegram
            if(telegramUsername != null && !telegramUsername.equals(existingUser.getTelegramUsername())) {
                existingUser.setTelegramUsername(telegramUsername);
            }
            // Consider logic for updating fullNameFromTelegram if it's different and user allows
            logger.info("Telegram user logged in: {}", telegramId);
            return userRepository.save(existingUser);
        } else {
            User newUser = User.builder()
                    .telegramId(telegramId)
                    .telegramUsername(telegramUsername)
                    .fullName(fullNameFromTelegram)
                    .registrationMethod(User.RegistrationMethod.TELEGRAM)
                    .accountStatus(AccountStatus.ACTIVE)
                    .telegramVerified(true)
                    .emailVerified(false) // Assuming no email provided at this stage
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            logger.info("New Telegram user registered: {}", telegramId);
            return userRepository.save(newUser);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserById(String userId) {
        return userRepository.findById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    @Override
    public User updateUserProfile(String userId,
                                  Map<String, String> fullName,
                                  Map<String, String> bio,
                                  List<User.ProfilePicture> profilePictures,
                                  List<String> skills,
                                  Map<String, String> publicContactDetails,
                                  List<User.SocialProfileLink> linkedSocialProfiles
                                  // ... other parameters for resumeDetailsText, resumeFileUrl etc.
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        if (fullName != null) user.setFullName(fullName);
        if (bio != null) user.setBio(bio);
        if (profilePictures != null) user.setProfilePictures(profilePictures); // Handle picture update logic (e.g. setting 'current')
        if (skills != null) user.setSkills(skills);
        if (publicContactDetails != null) user.setPublicContactDetails(publicContactDetails);
        if (linkedSocialProfiles != null) user.setLinkedSocialProfiles(linkedSocialProfiles);
        // Update other fields...

        user.setUpdatedAt(LocalDateTime.now());
        logger.info("User profile updated for userId: {}", userId);
        return userRepository.save(user);
    }

    @Override
    public User updateNotificationPreferences(String userId, Map<String, Object> notificationPreferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        user.setNotificationPreferences(notificationPreferences);
        user.setUpdatedAt(LocalDateTime.now());
        logger.info("Notification preferences updated for userId: {}", userId);
        return userRepository.save(user);
    }

    @Override
    public User updatePrivacySettings(String userId, Map<String, Object> privacySettings) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        user.setPrivacySettings(privacySettings);
        user.setUpdatedAt(LocalDateTime.now());
        logger.info("Privacy settings updated for userId: {}", userId);
        return userRepository.save(user);
    }

    @Override
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ValidationException("Incorrect old password.");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new ValidationException("New password must be at least 8 characters long.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Password changed for userId: {}", userId);
        return true;
    }

    // @Override
    // public boolean verifyEmail(String token) {
    //     // 1. Find user by token (you'd need to store token with user or in a separate table)
    //     // 2. Check token validity (not expired, etc.)
    //     // 3. Update user status: user.setEmailVerified(true); user.setAccountStatus(AccountStatus.ACTIVE);
    //     // 4. Invalidate token
    //     // This is a placeholder, actual implementation depends on token strategy.
    //     logger.info("Verifying email with token: {}", token);
    //     throw new UnsupportedOperationException("Email verification not fully implemented yet.");
    // }

    @Override
    // @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')") // Example of Phase 1 RBAC
    public User updateUserAccountStatus(String userId, AccountStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        user.setAccountStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        logger.info("Account status for userId {} updated to {}", userId, newStatus);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    // @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Pageable pageable) {
        // For Phase 1, this could be a simple search on email or full name.
        // A more sophisticated search would require text indexes on MongoDB or using Elasticsearch.
        // Example: (This is a naive search, not efficient for i18n or large datasets)
        // return userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(query, query, pageable);
        // You would need to adapt your UserRepository for this.
        // For now, let's return all users if query is blank, or search by email.
        if (!StringUtils.hasText(query)) {
            return userRepository.findAll(pageable);
        }
        // This requires a method like findByEmailContainingIgnoreCase in UserRepository
        // return userRepository.findByEmailContainingIgnoreCase(query, pageable);
        logger.warn("SearchUsers basic implementation used. Consider enhancing for production. Query: {}", query);
        return userRepository.findAll(pageable); // Placeholder for actual search logic
    }
}