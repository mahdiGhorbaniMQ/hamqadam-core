package ir.hamqadam.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ir.hamqadam.core.controller.dto.admin.AdminUserResponseDTO;
import ir.hamqadam.core.controller.dto.admin.SystemSettingDTO;
import ir.hamqadam.core.controller.dto.admin.SystemSettingsUpdateRequestDTO;
import ir.hamqadam.core.controller.dto.user.UserAccountStatusUpdateRequestDTO;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.UserService;
// import ir.hamqadam.core.service.SystemSettingsService; // If you had a dedicated service

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;

/**
 * API/Controller tests for {@link AdminController}.
 * Uses MockMvc to simulate HTTP requests and verify responses for admin operations.
 * Services are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class AdminControllerIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(AdminControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // @MockBean
    // private SystemSettingsService systemSettingsService; // If using a dedicated service

    private User mockUserForAdminView;
    private final String MOCK_ADMIN_EMAIL = "sysadmin@example.com";
    private final String TARGET_USER_ID = "user-to-manage-123";

    @BeforeAll
    static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for AdminControllerIntegrationTest");

        Map<String, String> fullName = new HashMap<>();
        fullName.put("en", "Managed User");
        fullName.put("fa", "کاربر مدیریت شده");

        mockUserForAdminView = User.builder()
                .userId(TARGET_USER_ID)
                .email("managed.user@example.com")
                .fullName(fullName)
                .accountStatus(User.AccountStatus.ACTIVE)
                .registrationMethod(User.RegistrationMethod.EMAIL)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .lastLoginAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Authorized as SYSTEM_ADMIN - Success")
    @WithMockUser(username = MOCK_ADMIN_EMAIL, roles = {"SYSTEM_ADMIN"})
    void getAllUsers_whenSystemAdmin_shouldReturnUserList() throws Exception {
        testLogger.info("Test: getAllUsers_whenSystemAdmin_shouldReturnUserList");
        // Arrange
        Page<User> userPage = new PageImpl<>(List.of(mockUserForAdminView), PageRequest.of(0, 20), 1);
        when(userService.findAllUsers(any(Pageable.class))).thenReturn(userPage);

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId", is(TARGET_USER_ID)))
                .andExpect(jsonPath("$.content[0].email", is(mockUserForAdminView.getEmail())))
                .andExpect(jsonPath("$.totalElements", is(1)));

        testLogger.info("Admin list users API call successful.");
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Authorized as regular USER - Forbidden")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void getAllUsers_whenRegularUser_shouldReturnForbidden() throws Exception {
        testLogger.info("Test: getAllUsers_whenRegularUser_shouldReturnForbidden");
        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON));
        // Assert
        resultActions.andExpect(status().isForbidden());
        testLogger.warn("Admin list users API call failed with 403 Forbidden for regular user, as expected.");
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Unauthenticated - Unauthorized")
    @WithAnonymousUser // Simulates an unauthenticated (anonymous) user
    void getAllUsers_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        testLogger.info("Test: getAllUsers_whenUnauthenticated_shouldReturnUnauthorized");
        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON));
        // Assert
        resultActions.andExpect(status().isUnauthorized());
        testLogger.warn("Admin list users API call failed with 401 Unauthorized for anonymous user, as expected.");
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{userId}/status - Authorized as SYSTEM_ADMIN - Success")
    @WithMockUser(username = MOCK_ADMIN_EMAIL, roles = {"SYSTEM_ADMIN"})
    void updateUserAccountStatus_whenSystemAdmin_shouldUpdateStatus() throws Exception {
        testLogger.info("Test: updateUserAccountStatus_whenSystemAdmin_shouldUpdateStatus");
        // Arrange
        UserAccountStatusUpdateRequestDTO statusUpdateRequest = new UserAccountStatusUpdateRequestDTO();
        statusUpdateRequest.setAccountStatus(User.AccountStatus.SUSPENDED);

        User updatedUser = User.builder()
                .userId(TARGET_USER_ID)
                .email(mockUserForAdminView.getEmail())
                .fullName(mockUserForAdminView.getFullName())
                .accountStatus(User.AccountStatus.SUSPENDED) // The new status
                .build();
        when(userService.updateUserAccountStatus(TARGET_USER_ID, User.AccountStatus.SUSPENDED))
                .thenReturn(updatedUser);

        // Act
        ResultActions resultActions = mockMvc.perform(put("/api/v1/admin/users/{userId}/status", TARGET_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdateRequest)));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", is(TARGET_USER_ID)))
                .andExpect(jsonPath("$.accountStatus", is(User.AccountStatus.SUSPENDED.toString())));

        testLogger.info("Admin update user status API call successful for user ID: {}", TARGET_USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings - Authorized as SYSTEM_ADMIN - Success (Placeholder Test)")
    @WithMockUser(username = MOCK_ADMIN_EMAIL, roles = {"SYSTEM_ADMIN"})
    void getSystemSettings_whenSystemAdmin_shouldReturnSettings() throws Exception {
        testLogger.info("Test: getSystemSettings_whenSystemAdmin_shouldReturnSettings (Placeholder)");
        // Arrange
        // If using SystemSettingsService:
        // List<SystemSetting> mockSettings = List.of(new SystemSetting("site_name", "Test Site", null, "STRING", null));
        // when(systemSettingsService.getAllSettings()).thenReturn(mockSettings.stream().map(this::convertToSystemSettingDTO).collect(Collectors.toList()));
        // For the controller's current placeholder implementation, no service mocking is needed for GET.

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/admin/settings")
                .contentType(MediaType.APPLICATION_JSON));

        // Assert (based on AdminController's placeholder implementation)
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3))) // Matches the placeholder data in controller
                .andExpect(jsonPath("$[0].key", is("site_name")))
                .andExpect(jsonPath("$[0].value", is("hamqadam Platform")));

        testLogger.info("Admin get system settings API call successful (using placeholder data).");
    }

    @Test
    @DisplayName("PUT /api/v1/admin/settings - Authorized as SYSTEM_ADMIN - Success (Placeholder Test)")
    @WithMockUser(username = MOCK_ADMIN_EMAIL, roles = {"SYSTEM_ADMIN"})
    void updateSystemSettings_whenSystemAdmin_shouldReturnSuccessMessage() throws Exception {
        testLogger.info("Test: updateSystemSettings_whenSystemAdmin_shouldReturnSuccessMessage (Placeholder)");
        // Arrange
        SystemSettingDTO settingToUpdate = new SystemSettingDTO("site_name", "Updated Hamqadam Site", new HashMap<>());
        SystemSettingsUpdateRequestDTO updateRequest = new SystemSettingsUpdateRequestDTO();
        updateRequest.setSettings(List.of(settingToUpdate));

        // If using SystemSettingsService:
        // when(systemSettingsService.updateSettings(anyList())).thenReturn(List.of(convertToSystemSettingModel(settingToUpdate)));

        // Act
        ResultActions resultActions = mockMvc.perform(put("/api/v1/admin/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("System settings updated successfully.")));

        testLogger.info("Admin update system settings API call successful (using placeholder logic).");
    }

    // --- Helper method for DTO conversion (Placeholder for AdminUserResponseDTO) ---
    private AdminUserResponseDTO convertToAdminUserResponseDTO(User user) {
        if (user == null) return null;
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
                .teamMemberships(user.getTeamMemberships())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
    // --- Helper method for DTO conversion (Placeholder for SystemSettingDTO if needed from model) ---
    // private SystemSettingDTO convertToSystemSettingDTO(SystemSetting setting) { /* ... */ }
    // private SystemSetting convertToSystemSettingModel(SystemSettingDTO dto) { /* ... */ }
}