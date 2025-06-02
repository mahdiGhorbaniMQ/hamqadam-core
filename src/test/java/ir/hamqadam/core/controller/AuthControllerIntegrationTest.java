package ir.hamqadam.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // For serializing request body to JSON
import ir.hamqadam.core.controller.dto.auth.UserRegistrationRequest;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// To test only the web layer (controller) and mock services: use @WebMvcTest(AuthController.class)
// To test with more of the Spring context (e.g. security filters, but still possibly mocking services):
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is; // For JSONPath assertions

/**
 * API/Controller tests for {@link AuthController}.
 * Uses MockMvc to simulate HTTP requests and verify responses.
 * The service layer (UserService) is mocked to isolate controller logic.
 * If you want to test controller + service integration, don't mock the service and use @SpringBootTest.
 */
@SpringBootTest // Loads full context, good for testing with security filters
@AutoConfigureMockMvc // Configures MockMvc
// Alternatively, for a lighter test focusing only on the controller and its direct interactions:
// @WebMvcTest(AuthController.class)
// @Import(SecurityConfig.class) // If SecurityConfig is needed and not auto-loaded by WebMvcTest
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class AuthControllerIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(AuthControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc; // For sending HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // For converting objects to JSON strings

    @MockBean // Creates a Mockito mock for UserService and registers it in Spring context
    private UserService userService;
    // JwtTokenProvider and AuthenticationManager are also dependencies of AuthController.
    // If using @WebMvcTest, they would need to be @MockBean too if not part of SecurityConfig auto-loaded.
    // If using @SpringBootTest, they are part of the context.

    private UserRegistrationRequest registrationRequest;
    private User registeredUser;

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for AuthControllerIntegrationTest");
        Map<String, String> fullName = new HashMap<>();
        fullName.put("en", "API Test User");
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setFullName(fullName);
        registrationRequest.setEmail("api.test@example.com");
        registrationRequest.setPassword("securePassword123");

        registeredUser = User.builder()
                .userId("user123")
                .email(registrationRequest.getEmail())
                .fullName(registrationRequest.getFullName())
                .accountStatus(User.AccountStatus.PENDING_VERIFICATION)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/email - Success")
    void registerUserByEmail_whenValidRequest_shouldReturnSuccessMessage() throws Exception {
        testLogger.info("Test: registerUserByEmail_whenValidRequest_shouldReturnSuccessMessage");
        // Arrange: Mock the userService.registerNewUserByEmail to return the sample registeredUser
        when(userService.registerNewUserByEmail(anyMap(), anyString(), anyString()))
                .thenReturn(registeredUser);

        // Act: Perform the POST request
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/register/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)));

        // Assert: Check the HTTP response status and body
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("User registered successfully! Please check your email to verify.")));

        testLogger.info("Registration API call successful.");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/email - Invalid Email Format")
    void registerUserByEmail_whenInvalidEmailFormat_shouldReturnBadRequest() throws Exception {
        testLogger.info("Test: registerUserByEmail_whenInvalidEmailFormat_shouldReturnBadRequest");
        // Arrange: Modify the request to have an invalid email
        registrationRequest.setEmail("invalid-email");

        // Act: Perform the POST request
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/register/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)));

        // Assert: Check for Bad Request (400) and validation error message
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // This path assumes your GlobalExceptionHandler returns validation errors in a "validationErrors.email" field
                // or a general "message" field if only one error. Adjust based on your GlobalExceptionHandler response.
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors.email", is("Email should be valid")));

        testLogger.warn("Registration API call failed with bad request due to invalid email, as expected.");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/email - Password Too Short")
    void registerUserByEmail_whenPasswordTooShort_shouldReturnBadRequest() throws Exception {
        testLogger.info("Test: registerUserByEmail_whenPasswordTooShort_shouldReturnBadRequest");
        registrationRequest.setPassword("short");

        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/register/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)));

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors.password", is("Password must be at least 8 characters long")));
        testLogger.warn("Registration API call failed with bad request due to short password, as expected.");
    }


    // Add more API tests for AuthController:
    // - Successful login (/api/v1/auth/login/email)
    // - Failed login (wrong credentials)
    // - Telegram login/registration
    // - Password reset flow
    //
    // You would create similar test classes for UserController, TeamController, etc.
    // For endpoints requiring authentication, you'd need to simulate an authenticated user,
    // often by first calling the login endpoint to get a token and then including that token
    // in subsequent requests' Authorization header, or by using Spring Security Test support
    // like @WithMockUser or by manually setting the SecurityContext.
}