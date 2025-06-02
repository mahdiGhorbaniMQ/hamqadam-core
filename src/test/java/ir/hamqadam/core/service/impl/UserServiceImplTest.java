package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link UserServiceImpl} class.
 * These tests focus on the business logic of the service layer in isolation,
 * with dependencies like repositories and encoders mocked.
 */
@ExtendWith(MockitoExtension.class) // Integrates Mockito with JUnit 5
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class UserServiceImplTest {

    private static final Logger testLogger = LoggerFactory.getLogger(UserServiceImplTest.class);

    @Mock // Creates a mock instance of UserRepository
    private UserRepository userRepository;

    @Mock // Creates a mock instance of PasswordEncoder
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Creates an instance of UserServiceImpl and injects the mocks into it
    private UserServiceImpl userService;

    private Map<String, String> sampleFullName;
    private String sampleEmail;
    private String samplePassword;
    private String encodedPassword;

    /**
     * Sets up common test data before each test method.
     */
    @BeforeEach
    void setUp() {
        testLogger.info("Setting up test data for UserServiceImplTest");
        sampleFullName = new HashMap<>();
        sampleFullName.put("en", "Test User");
        sampleFullName.put("fa", "کاربر تستی");

        sampleEmail = "test@example.com";
        samplePassword = "password123";
        encodedPassword = "encodedPassword123"; // Dummy encoded password

        // Configure mock behavior (can also be done within each test method for specificity)
        lenient().when(passwordEncoder.encode(samplePassword)).thenReturn(encodedPassword);
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            if (userToSave.getUserId() == null) {
                userToSave.setUserId("generated-id-" + System.nanoTime()); // Simulate ID generation
            }
            userToSave.setCreatedAt(LocalDateTime.now()); // Simulate @CreatedDate
            userToSave.setUpdatedAt(LocalDateTime.now()); // Simulate @LastModifiedDate
            return userToSave;
        });
    }

    @Test
    @DisplayName("Should register new user successfully when email is not taken")
    void registerNewUserByEmail_whenEmailNotTaken_shouldSucceed() {
        testLogger.info("Test: registerNewUserByEmail_whenEmailNotTaken_shouldSucceed");

        // Arrange: Configure mock to indicate email is not taken
        when(userRepository.existsByEmail(sampleEmail)).thenReturn(false);

        // Act: Call the service method
        User registeredUser = userService.registerNewUserByEmail(sampleFullName, sampleEmail, samplePassword);

        // Assert: Verify the results
        assertNotNull(registeredUser, "Registered user should not be null");
        assertNotNull(registeredUser.getUserId(), "User ID should be generated");
        assertEquals(sampleEmail, registeredUser.getEmail(), "Email should match");
        assertEquals(encodedPassword, registeredUser.getPasswordHash(), "Password should be encoded");
        assertEquals(sampleFullName, registeredUser.getFullName(), "Full name should match");
        assertEquals(User.RegistrationMethod.EMAIL, registeredUser.getRegistrationMethod(), "Registration method should be EMAIL");
        assertEquals(User.AccountStatus.PENDING_VERIFICATION, registeredUser.getAccountStatus(), "Account status should be PENDING_VERIFICATION");
        assertFalse(registeredUser.isEmailVerified(), "Email should not be verified initially");

        // Verify that userRepository.save was called exactly once with any User object
        verify(userRepository, times(1)).save(any(User.class));
        // Verify that passwordEncoder.encode was called with the samplePassword
        verify(passwordEncoder, times(1)).encode(samplePassword);

        testLogger.info("User registered successfully with ID: {}", registeredUser.getUserId());
    }

    @Test
    @DisplayName("Should throw ValidationException when registering with an existing email")
    void registerNewUserByEmail_whenEmailIsTaken_shouldThrowValidationException() {
        testLogger.info("Test: registerNewUserByEmail_whenEmailIsTaken_shouldThrowValidationException");

        // Arrange: Configure mock to indicate email is already taken
        when(userRepository.existsByEmail(sampleEmail)).thenReturn(true);

        // Act & Assert: Call the service method and assert that it throws ValidationException
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.registerNewUserByEmail(sampleFullName, sampleEmail, samplePassword);
        });

        assertEquals("Email address already in use: " + sampleEmail, exception.getMessage(), "Exception message should match");

        // Verify that userRepository.save was never called
        verify(userRepository, never()).save(any(User.class));
        // Verify that passwordEncoder.encode was never called (as it failed before encoding)
        verify(passwordEncoder, never()).encode(anyString());

        testLogger.warn("ValidationException thrown as expected for existing email: {}", sampleEmail);
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid password (e.g., too short)")
    void registerNewUserByEmail_whenPasswordIsInvalid_shouldThrowValidationException() {
        testLogger.info("Test: registerNewUserByEmail_whenPasswordIsInvalid_shouldThrowValidationException");
        String shortPassword = "short";

        // Arrange: Email is not taken
        when(userRepository.existsByEmail(sampleEmail)).thenReturn(false);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.registerNewUserByEmail(sampleFullName, sampleEmail, shortPassword);
        });

        assertEquals("Password must be at least 8 characters long.", exception.getMessage(), "Exception message for short password should match");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());

        testLogger.warn("ValidationException thrown as expected for short password.");
    }

    // Add more unit tests for other methods in UserServiceImpl:
    // - registerOrLoginTelegramUser (new user, existing user)
    // - findUserById, findUserByEmail, findUserByTelegramId (found, not found)
    // - updateUserProfile (success, user not found)
    // - changePassword (success, old password mismatch, user not found)
    // - updateUserAccountStatus (success, user not found)
    // etc.
}