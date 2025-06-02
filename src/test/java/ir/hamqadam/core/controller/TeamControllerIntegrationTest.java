package ir.hamqadam.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.hamqadam.core.controller.dto.team.TeamCreationRequestDTO;
import ir.hamqadam.core.controller.dto.team.TeamResponseDTO;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.TeamService;
import ir.hamqadam.core.service.UserService; // TeamController uses this to get User from UserDetails

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser; // For simulating authenticated user
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * API/Controller tests for {@link TeamController}.
 * Uses MockMvc to simulate HTTP requests and verify responses for team creation.
 * Services are mocked to isolate controller logic and interactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class TeamControllerIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(TeamControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamService teamService;

    @MockBean
    private UserService userService; // TeamController autowires this

    private TeamCreationRequestDTO teamCreationRequest;
    private User mockAuthenticatedUser;
    private Team mockCreatedTeam;
    private Map<String,String> teamNameI18n;

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for TeamControllerIntegrationTest");

        teamNameI18n = new HashMap<>();
        teamNameI18n.put("en", "Controller Test Team");
        teamNameI18n.put("fa", "تیم تست کنترلر");

        teamCreationRequest = new TeamCreationRequestDTO();
        teamCreationRequest.setTeamName(teamNameI18n);
        teamCreationRequest.setTeamHandle("controller_test_team");
        teamCreationRequest.setIntroductoryPostId("post-for-team-creation");
        teamCreationRequest.setDescription(Map.of("en", "A team created via controller test"));
        teamCreationRequest.setVisibility(Team.TeamVisibility.PUBLIC);
        teamCreationRequest.setMembershipApprovalRequired(true);

        mockAuthenticatedUser = User.builder()
                .userId("auth-user-123")
                .email("authenticated@example.com")
                .fullName(Map.of("en", "Authenticated User"))
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        Team.TeamMember creatorAsMember = Team.TeamMember.builder()
                .userId(mockAuthenticatedUser.getUserId())
                .roles(Collections.singletonList("ADMIN"))
                .joinDate(LocalDateTime.now())
                .statusInTeam(Team.MemberStatus.ACTIVE)
                .build();

        mockCreatedTeam = Team.builder()
                .teamId("team-generated-456")
                .teamName(teamNameI18n)
                .teamHandle(teamCreationRequest.getTeamHandle())
                .introductoryPostId(teamCreationRequest.getIntroductoryPostId())
                .description(teamCreationRequest.getDescription())
                .visibility(teamCreationRequest.getVisibility())
                .membershipApprovalRequired(teamCreationRequest.isMembershipApprovalRequired())
                .creatorUserId(mockAuthenticatedUser.getUserId())
                .members(Collections.singletonList(creatorAsMember))
                .teamStatus(Team.TeamStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/teams - Create Team Successfully")
    @WithMockUser(username = "authenticated@example.com", roles = {"USER"}) // Simulate an authenticated user
    void createTeam_whenAuthenticatedAndValidRequest_shouldReturnCreatedTeam() throws Exception {
        testLogger.info("Test: createTeam_whenAuthenticatedAndValidRequest_shouldReturnCreatedTeam");

        // Arrange
        // Mock userService to return the authenticated user
        when(userService.findUserByEmail("authenticated@example.com")).thenReturn(Optional.of(mockAuthenticatedUser));

        // Mock teamService.createTeam to return the mockCreatedTeam when called with expected parameters
        when(teamService.createTeam(
                anyMap(), // teamName
                anyString(), // teamHandle
                anyString(), // introductoryPostId
                anyMap(), // description
                any(Team.TeamVisibility.class),
                anyBoolean(), // membershipApprovalRequired
                any(User.class) // actingUser
        )).thenReturn(mockCreatedTeam);

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(teamCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.teamId", is(mockCreatedTeam.getTeamId())))
                .andExpect(jsonPath("$.teamName.en", is(teamNameI18n.get("en"))))
                .andExpect(jsonPath("$.teamHandle", is(teamCreationRequest.getTeamHandle())))
                .andExpect(jsonPath("$.creatorUserId", is(mockAuthenticatedUser.getUserId())));

        testLogger.info("Team creation API call successful for handle: {}", teamCreationRequest.getTeamHandle());
    }

    @Test
    @DisplayName("POST /api/v1/teams - Unauthenticated Access Should Fail")
    void createTeam_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        testLogger.info("Test: createTeam_whenUnauthenticated_shouldReturnUnauthorized");
        // Arrange: No @WithMockUser, so request is unauthenticated

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(teamCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isUnauthorized()); // HTTP 401
        // Your JwtAuthenticationEntryPoint should customize this response

        testLogger.warn("Team creation API call failed with 401 Unauthorized, as expected for unauthenticated user.");
    }

    @Test
    @DisplayName("POST /api/v1/teams - Invalid Request Body (e.g., blank handle) Should Return Bad Request")
    @WithMockUser(username = "authenticated@example.com", roles = {"USER"})
    void createTeam_whenInvalidRequestBody_shouldReturnBadRequest() throws Exception {
        testLogger.info("Test: createTeam_whenInvalidRequestBody_shouldReturnBadRequest");
        // Arrange
        teamCreationRequest.setTeamHandle(""); // Invalid: blank handle

        // Mock userService because the controller will try to fetch the actingUser
        when(userService.findUserByEmail("authenticated@example.com")).thenReturn(Optional.of(mockAuthenticatedUser));
        // No need to mock teamService.createTeam as validation should fail before it's called.

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(teamCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isBadRequest()) // HTTP 400
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors.teamHandle", notNullValue())); // Check that there's an error for teamHandle

        testLogger.warn("Team creation API call failed with 400 Bad Request due to invalid input, as expected.");
    }

    // Add more API tests for TeamController:
    // - Get team details (publicly accessible vs. private with permissions)
    // - Update team (success, unauthorized, team not found)
    // - Invite user (success, unauthorized, validation errors like user already member)
    // - Respond to invitation
    // - Request to join
    // - Process join request
    // - Manage members (change role, remove)
    // - Leave team
    // - Search teams
    // Ensure to test different roles and permissions using @WithMockUser or other Spring Security Test mechanisms.
}