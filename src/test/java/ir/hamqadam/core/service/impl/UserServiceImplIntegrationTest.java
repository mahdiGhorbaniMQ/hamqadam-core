package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.UserRepository;
import ir.hamqadam.core.service.UserService; // Import the interface
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder; // Autowired from SecurityConfig
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link UserServiceImpl}.
 * These tests run with a Spring context and interact with an embedded MongoDB instance
 * to verify the service layer's integration with the data persistence layer.
 */
@SpringBootTest // Loads the full Spring application context
@ActiveProfiles("test") // Optional: Use a specific test profile (e.g., for different DB config)
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class UserServiceImplIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(UserServiceImplIntegrationTest.class);

    @Autowired
    private UserService userService; // Autowire the service (implementation will be picked up)

    @Autowired
    private UserRepository userRepository; // For setup/verification directly with DB

    @Autowired
    private PasswordEncoder passwordEncoder; // To verify password encoding

    private Map<String, String> sampleFullName;
    private String sampleEmail;
    private String samplePassword;

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for UserServiceImplIntegrationTest");
        // Clean up the repository before each test to ensure independence
        userRepository.deleteAll(); // Important for integration tests!

        sampleFullName = new HashMap<>();
        sampleFullName.put("en", "Integration Test User");
        sampleFullName.put("fa", "کاربر تست یکپارچه‌سازی"); // Will be stored as is
        sampleEmail = "integration.test@example.com";
        samplePassword = "password123Secure";
    }

    @AfterEach
    void tearDown() {
        testLogger.info("Tearing down data after UserServiceImplIntegrationTest");
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: Should register new user successfully and persist to DB")
    void registerNewUserByEmail_whenEmailNotTaken_shouldPersistUser() {
        testLogger.info("Test: registerNewUserByEmail_whenEmailNotTaken_shouldPersistUser");

        // Act
        User registeredUser = userService.registerNewUserByEmail(sampleFullName, sampleEmail, samplePassword);

        // Assert: Check service response
        assertNotNull(registeredUser);
        assertNotNull(registeredUser.getUserId());
        assertEquals(sampleEmail, registeredUser.getEmail());
        assertTrue(passwordEncoder.matches(samplePassword, registeredUser.getPasswordHash()), "Stored password should match the encoded version of original");
        assertEquals(User.AccountStatus.PENDING_VERIFICATION, registeredUser.getAccountStatus());

        // Assert: Verify directly from the database
        Optional<User> foundUserOpt = userRepository.findById(registeredUser.getUserId());
        assertTrue(foundUserOpt.isPresent(), "User should be found in the database");
        User foundUser = foundUserOpt.get();
        assertEquals(sampleEmail, foundUser.getEmail());
        assertEquals(sampleFullName, foundUser.getFullName());

        testLogger.info("User {} persisted successfully with ID: {}", foundUser.getEmail(), foundUser.getUserId());
    }

    @Test
    @DisplayName("Integration Test: Should throw ValidationException when email is already persisted")
    void registerNewUserByEmail_whenEmailExistsInDb_shouldThrowValidationException() {
        testLogger.info("Test: registerNewUserByEmail_whenEmailExistsInDb_shouldThrowValidationException");

        // Arrange: First, save a user with the same email directly to simulate existing data
        User existingUser = User.builder()
                .email(sampleEmail)
                .passwordHash(passwordEncoder.encode("someOtherPassword"))
                .fullName(sampleFullName)
                .registrationMethod(User.RegistrationMethod.EMAIL)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();
        userRepository.save(existingUser);
        testLogger.info("Pre-saved user with email: {}", sampleEmail);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.registerNewUserByEmail(sampleFullName, sampleEmail, samplePassword);
        });

        assertEquals("Email address already in use: " + sampleEmail, exception.getMessage());

        // Verify that only one user with this email exists (the one we pre-saved)
        long userCount = userRepository.findAll().stream().filter(u -> u.getEmail().equals(sampleEmail)).count();
        assertEquals(1, userCount, "Only the pre-saved user should exist with this email");

        testLogger.warn("ValidationException correctly thrown for existing email: {}", sampleEmail);
    }

    // Add more integration tests for:
    // - Login scenarios (requires security context setup if testing secured service methods)
    // - Profile updates and verifying changes in DB
    // - Interactions between multiple services if applicable
    // - Testing @Transactional behavior (e.g., rollback on error)
}