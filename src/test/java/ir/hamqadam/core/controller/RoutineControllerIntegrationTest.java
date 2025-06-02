package ir.hamqadam.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // For LocalDateTime serialization
import ir.hamqadam.core.controller.dto.routine.RoutineCreationRequestDTO;
import ir.hamqadam.core.controller.dto.routine.RoutineCreatorInfoDTO;
import ir.hamqadam.core.controller.dto.routine.RoutineResponseDTO;
import ir.hamqadam.core.controller.dto.user.UserSummaryDTO; // For enriching response DTOs
import ir.hamqadam.core.model.Routine;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.RoutineService;
import ir.hamqadam.core.service.UserService;
import ir.hamqadam.core.service.TeamService; // If team is a creator or participant

import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * API/Controller tests for {@link RoutineController}.
 * Uses MockMvc to simulate HTTP requests and verify responses for routine operations.
 * Services are mocked to isolate controller logic and interactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class RoutineControllerIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(RoutineControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper needs JavaTimeModule for LocalDateTime
    private static ObjectMapper objectMapper;


    @MockBean
    private RoutineService routineService;

    @MockBean
    private UserService userService;

    @MockBean
    private TeamService teamService; // Mock if routines can have team creators/participants

    private RoutineCreationRequestDTO routineCreationRequest;
    private User mockAuthenticatedUser;
    private Routine mockCreatedRoutine;
    private Map<String, String> routineTitleI18n;
    private LocalDateTime futureStartDateTime;

    private final String MOCK_USER_EMAIL = "routine.creator@example.com";
    private final String MOCK_USER_ID = "user-routine-creator-123";
    private final String MOCK_POST_ID = "desc-post-for-routine-123";

    @BeforeAll
    static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Crucial for LocalDateTime
    }

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for RoutineControllerIntegrationTest");

        futureStartDateTime = LocalDateTime.now().plusDays(1).withNano(0); // Remove nanos for cleaner JSON comparison

        routineTitleI18n = new HashMap<>();
        routineTitleI18n.put("en", "Daily Standup Routine");
        routineTitleI18n.put("fa", "روتین استندآپ روزانه");

        RoutineCreatorInfoDTO creatorInfoDTO = new RoutineCreatorInfoDTO(Routine.CreatorType.USER, MOCK_USER_ID);

        routineCreationRequest = new RoutineCreationRequestDTO();
        routineCreationRequest.setTitle(routineTitleI18n);
        routineCreationRequest.setDescriptivePostId(MOCK_POST_ID);
        routineCreationRequest.setCreatorInfo(creatorInfoDTO);
        routineCreationRequest.setScheduleType(Routine.ScheduleType.RECURRING);
        routineCreationRequest.setStartDatetime(futureStartDateTime);
        routineCreationRequest.setRecurrenceRule("FREQ=DAILY;INTERVAL=1");
        routineCreationRequest.setDuration("PT15M"); // ISO 8601 duration for 15 minutes
        routineCreationRequest.setTimezone("Asia/Tehran");
        routineCreationRequest.setVisibility(Routine.RoutineVisibility.PRIVATE_TO_PARTICIPANTS);

        mockAuthenticatedUser = User.builder()
                .userId(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .fullName(Map.of("en", "Routine Creator"))
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        Routine.CreatorInfo creatorInfoModel = new Routine.CreatorInfo(
                Routine.CreatorType.USER,
                MOCK_USER_ID,
                MOCK_USER_ID // Acting user
        );

        mockCreatedRoutine = Routine.builder()
                .routineId("routine-generated-abc")
                .title(routineTitleI18n)
                .descriptivePostId(MOCK_POST_ID)
                .creatorInfo(creatorInfoModel)
                .scheduleType(Routine.ScheduleType.RECURRING)
                .startDatetime(futureStartDateTime)
                .recurrenceRule("FREQ=DAILY;INTERVAL=1")
                .duration("PT15M")
                .timezone("Asia/Tehran")
                .status(Routine.RoutineStatus.ACTIVE)
                .visibility(Routine.RoutineVisibility.PRIVATE_TO_PARTICIPANTS)
                .participants(new ArrayList<>()) // Initialize empty list
                .createdAt(LocalDateTime.now().withNano(0))
                .updatedAt(LocalDateTime.now().withNano(0))
                .nextOccurrenceDatetime(futureStartDateTime) // Simplified for mock
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/routines - Create Routine Successfully")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"})
    void createRoutine_whenAuthenticatedAndValidRequest_shouldReturnCreatedRoutine() throws Exception {
        testLogger.info("Test: createRoutine_whenAuthenticatedAndValidRequest_shouldReturnCreatedRoutine");

        // Arrange
        when(userService.findUserByEmail(MOCK_USER_EMAIL)).thenReturn(Optional.of(mockAuthenticatedUser));
        when(routineService.createRoutine(
                anyMap(), // title
                anyString(), // descriptivePostId
                any(Routine.CreatorInfo.class),
                any(Routine.ScheduleType.class),
                any(LocalDateTime.class), // startDatetime
                any(), // endDatetime (can be null)
                anyString(), // recurrenceRule
                anyString(), // duration
                anyString(), // timezone
                any(), // purposeOrGoal (can be null)
                any(), // locationOrPlatformDetails (can be null)
                any(), // initialParticipants (can be null)
                any(), // visibility (can be null, uses service default)
                any(), // initialStatus (can be null, uses service default)
                any(), // linkedProjectId (can be null)
                any(), // linkedTeamId (can be null)
                any(User.class) // actingUser
        )).thenReturn(mockCreatedRoutine);

        // Mock for DTO conversion - creator info
        when(userService.findUserById(MOCK_USER_ID)).thenReturn(Optional.of(mockAuthenticatedUser));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/routines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(routineCreationRequest)));

        testLogger.debug("Request body for createRoutine: {}", objectMapper.writeValueAsString(routineCreationRequest));
        testLogger.debug("Expected response structure for createRoutine (mocked): {}", objectMapper.writeValueAsString(RoutineResponseDTO.builder().routineId(mockCreatedRoutine.getRoutineId()).title(routineTitleI18n).build()));


        // Assert
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.routineId", is(mockCreatedRoutine.getRoutineId())))
                .andExpect(jsonPath("$.title.en", is(routineTitleI18n.get("en"))))
                .andExpect(jsonPath("$.creatorInfo.creatorId", is(MOCK_USER_ID)))
                .andExpect(jsonPath("$.scheduleType", is(Routine.ScheduleType.RECURRING.toString())))
                .andExpect(jsonPath("$.startDatetime", is(futureStartDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        testLogger.info("Routine creation API call successful for routine ID: {}", mockCreatedRoutine.getRoutineId());
    }

    @Test
    @DisplayName("POST /api/v1/routines - Invalid Request (e.g., start time in past)")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"})
    void createRoutine_whenStartTimeInPast_shouldReturnBadRequest() throws Exception {
        testLogger.info("Test: createRoutine_whenStartTimeInPast_shouldReturnBadRequest");
        // Arrange
        routineCreationRequest.setStartDatetime(LocalDateTime.now().minusDays(1)); // Start time in the past
        when(userService.findUserByEmail(MOCK_USER_EMAIL)).thenReturn(Optional.of(mockAuthenticatedUser));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/routines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(routineCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors.startDatetime", is("Start datetime must be now or in the future")));

        testLogger.warn("Routine creation API call failed with 400 Bad Request due to past start time, as expected.");
    }

    @Test
    @DisplayName("GET /api/v1/routines/{routineId} - Found")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"}) // User for potential visibility checks
    void getRoutineById_whenRoutineExists_shouldReturnRoutine() throws Exception {
        testLogger.info("Test: getRoutineById_whenRoutineExists_shouldReturnRoutine");
        // Arrange
        String routineId = mockCreatedRoutine.getRoutineId();
        when(routineService.findRoutineById(routineId)).thenReturn(Optional.of(mockCreatedRoutine));

        // Mock dependencies for DTO conversion if RoutineResponseDTO has enriched fields
        when(userService.findUserById(mockCreatedRoutine.getCreatorInfo().getCreatorId()))
                .thenReturn(Optional.of(mockAuthenticatedUser)); // Assuming creator is the mock user

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/routines/{routineId}", routineId)
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.routineId", is(routineId)))
                .andExpect(jsonPath("$.title.en", is(mockCreatedRoutine.getTitle().get("en"))))
                .andExpect(jsonPath("$.startDatetime", is(mockCreatedRoutine.getStartDatetime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        testLogger.info("Get routine by ID API call successful for ID: {}", routineId);
    }

    @Test
    @DisplayName("GET /api/v1/routines/{routineId} - Not Found")
    void getRoutineById_whenRoutineNotFound_shouldReturnNotFound() throws Exception {
        testLogger.info("Test: getRoutineById_whenRoutineNotFound_shouldReturnNotFound");
        // Arrange
        String nonExistentRoutineId = "non-existent-routine-id";
        when(routineService.findRoutineById(nonExistentRoutineId)).thenReturn(Optional.empty());

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/routines/{routineId}", nonExistentRoutineId)
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Routine not found with ID: " + nonExistentRoutineId)));
        testLogger.warn("Get routine by ID API call failed with 404 Not Found for ID: {}, as expected.", nonExistentRoutineId);
    }

    // Add more API tests for RoutineController:
    // - Update routine info (success, unauthorized, not found, validation errors)
    // - Change routine status
    // - Participant management: add, remove, RSVP (success, unauthorized, validation)
    // - List routines (my routines, by project, upcoming feed with filters)
    // - Test visibility rules for fetching routines.
    // - Test scenarios with different schedule types (SINGLE_OCCURRENCE, RECURRING).
}