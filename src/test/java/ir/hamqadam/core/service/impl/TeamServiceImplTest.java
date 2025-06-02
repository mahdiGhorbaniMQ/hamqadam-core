package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
// Assume Post model exists for introductoryPostId context, even if not directly fetched by TeamService in Phase 1
// import ir.hamqadam.core.model.Post;
import ir.hamqadam.core.repository.TeamRepository;
import ir.hamqadam.core.repository.UserRepository;
// import ir.hamqadam.core.repository.PostRepository;
// import ir.hamqadam.core.service.NotificationService; // If notifications were actively sent
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link TeamServiceImpl} class.
 * Dependencies are mocked to test the service logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class TeamServiceImplTest {

    private static final Logger testLogger = LoggerFactory.getLogger(TeamServiceImplTest.class);

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository; // Used to validate actingUser if createTeam took userId

    // @Mock
    // private PostRepository postRepository; // If createTeam validated introductoryPostId existence

    // @Mock
    // private NotificationService notificationService; // If createTeam sent notifications

    @InjectMocks
    private TeamServiceImpl teamService;

    private User actingUser;
    private Map<String, String> sampleTeamName;
    private String sampleTeamHandle;
    private String sampleIntroductoryPostId;
    private Map<String, String> sampleDescription;

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up test data for TeamServiceImplTest");

        actingUser = User.builder()
                .userId("user-creator-123")
                .email("creator@example.com")
                .fullName(Map.of("en", "Creator User"))
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        sampleTeamName = new HashMap<>();
        sampleTeamName.put("en", "Awesome Team");
        sampleTeamName.put("fa", "تیم فوق‌العاده");

        sampleTeamHandle = "awesome_team";
        sampleIntroductoryPostId = "post-intro-123";
        sampleDescription = new HashMap<>();
        sampleDescription.put("en", "This is an awesome team for awesome projects.");

        // Default mock behavior for saving a team
        lenient().when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team teamToSave = invocation.getArgument(0);
            if (teamToSave.getTeamId() == null) {
                teamToSave.setTeamId("team-generated-id-" + System.nanoTime());
            }
            teamToSave.setCreatedAt(LocalDateTime.now());
            teamToSave.setUpdatedAt(LocalDateTime.now());
            return teamToSave;
        });
    }

    @Test
    @DisplayName("Should create team successfully with valid inputs")
    void createTeam_withValidInputs_shouldSucceed() {
        testLogger.info("Test: createTeam_withValidInputs_shouldSucceed");

        // Arrange
        when(teamRepository.existsByTeamHandle(sampleTeamHandle)).thenReturn(false);
        // Assuming introductoryPostId validation is done elsewhere or not in this service method for Phase 1.
        // If it was, you'd mock postRepository:
        // when(postRepository.findById(sampleIntroductoryPostId)).thenReturn(Optional.of(new Post()));


        // Act
        Team createdTeam = teamService.createTeam(
                sampleTeamName,
                sampleTeamHandle,
                sampleIntroductoryPostId,
                sampleDescription,
                Team.TeamVisibility.PUBLIC,
                true,
                actingUser
        );

        // Assert
        assertNotNull(createdTeam, "Created team should not be null");
        assertNotNull(createdTeam.getTeamId(), "Team ID should be generated");
        assertEquals(sampleTeamName, createdTeam.getTeamName(), "Team name should match");
        assertEquals(sampleTeamHandle, createdTeam.getTeamHandle(), "Team handle should match");
        assertEquals(sampleIntroductoryPostId, createdTeam.getIntroductoryPostId(), "Intro post ID should match");
        assertEquals(actingUser.getUserId(), createdTeam.getCreatorUserId(), "Creator user ID should match");
        assertEquals(Team.TeamStatus.ACTIVE, createdTeam.getTeamStatus(), "Team status should be ACTIVE");
        assertNotNull(createdTeam.getMembers(), "Members list should not be null");
        assertEquals(1, createdTeam.getMembers().size(), "Team should have one member (the creator)");

        Team.TeamMember creatorMember = createdTeam.getMembers().get(0);
        assertEquals(actingUser.getUserId(), creatorMember.getUserId(), "Creator should be the member");
        assertTrue(creatorMember.getRoles().contains("ADMIN"), "Creator should have ADMIN role");
        assertEquals(Team.MemberStatus.ACTIVE, creatorMember.getStatusInTeam(), "Creator's member status should be ACTIVE");

        verify(teamRepository, times(1)).existsByTeamHandle(sampleTeamHandle);
        verify(teamRepository, times(1)).save(any(Team.class));

        testLogger.info("Team '{}' created successfully with ID: {}", createdTeam.getTeamHandle(), createdTeam.getTeamId());
    }

    @Test
    @DisplayName("Should throw ValidationException when team handle already exists")
    void createTeam_whenHandleExists_shouldThrowValidationException() {
        testLogger.info("Test: createTeam_whenHandleExists_shouldThrowValidationException");

        // Arrange
        when(teamRepository.existsByTeamHandle(sampleTeamHandle)).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            teamService.createTeam(
                    sampleTeamName,
                    sampleTeamHandle,
                    sampleIntroductoryPostId,
                    sampleDescription,
                    Team.TeamVisibility.PUBLIC,
                    true,
                    actingUser
            );
        });

        assertEquals("Team handle is invalid or already exists: " + sampleTeamHandle, exception.getMessage());
        verify(teamRepository, times(1)).existsByTeamHandle(sampleTeamHandle);
        verify(teamRepository, never()).save(any(Team.class));

        testLogger.warn("ValidationException thrown as expected for existing team handle: {}", sampleTeamHandle);
    }

    @Test
    @DisplayName("Should throw ValidationException when introductoryPostId is null or empty")
    void createTeam_whenIntroductoryPostIdIsNull_shouldThrowValidationException() {
        testLogger.info("Test: createTeam_whenIntroductoryPostIdIsNull_shouldThrowValidationException");

        // Arrange
        when(teamRepository.existsByTeamHandle(sampleTeamHandle)).thenReturn(false); // Assuming handle is unique

        // Act & Assert for null postId
        ValidationException nullException = assertThrows(ValidationException.class, () -> {
            teamService.createTeam(
                    sampleTeamName,
                    sampleTeamHandle,
                    null, // Null introductoryPostId
                    sampleDescription,
                    Team.TeamVisibility.PUBLIC,
                    true,
                    actingUser
            );
        });
        assertEquals("Introductory post ID is required.", nullException.getMessage());

        // Act & Assert for empty postId
        ValidationException emptyException = assertThrows(ValidationException.class, () -> {
            teamService.createTeam(
                    sampleTeamName,
                    sampleTeamHandle,
                    "", // Empty introductoryPostId
                    sampleDescription,
                    Team.TeamVisibility.PUBLIC,
                    true,
                    actingUser
            );
        });
        assertEquals("Introductory post ID is required.", emptyException.getMessage());

        verify(teamRepository, never()).save(any(Team.class));
        testLogger.warn("ValidationException thrown as expected for missing introductoryPostId.");
    }


    // Add more unit tests for other methods in TeamServiceImpl:
    // - updateTeamInfo (success, team not found, unauthorized)
    // - inviteUserToTeam (success, user already member, user not found, team not found, unauthorized)
    // - respondToTeamInvitation (accept, decline, invitation not found)
    // - requestToJoinTeam (success, team not public, already member)
    // - processMembershipRequest (approve, reject, request not found, unauthorized)
    // - updateTeamMemberRoles
    // - removeTeamMember (success, last admin scenario)
    // - leaveTeam
    // - changeTeamStatus
}