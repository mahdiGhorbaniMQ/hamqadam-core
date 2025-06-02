package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.TeamRepository;
import ir.hamqadam.core.repository.UserRepository;
// Assuming Post model exists for introductoryPostId context, and PostRepository for validation
// import ir.hamqadam.core.model.Post;
// import ir.hamqadam.core.repository.PostRepository;
import ir.hamqadam.core.service.TeamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link TeamServiceImpl}.
 * These tests verify the service layer's integration with the data persistence layer (MongoDB)
 * for Team entity operations.
 */
@SpringBootTest
@ActiveProfiles("test") // Optional: use a specific test profile
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class TeamServiceImplIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(TeamServiceImplIntegrationTest.class);

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository; // To create the actingUser

    // @Autowired
    // private PostRepository postRepository; // If validating introductoryPostId by fetching Post

    private User actingUser;
    private Map<String, String> sampleTeamName;
    private String sampleTeamHandle;
    private String validIntroductoryPostId;
    private Map<String, String> sampleDescription;

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for TeamServiceImplIntegrationTest");
        // Clean up repositories before each test
        teamRepository.deleteAll();
        userRepository.deleteAll();
        // postRepository.deleteAll(); // If using PostRepository

        // Create and save the acting user
        Map<String, String> actingUserFullName = new HashMap<>();
        actingUserFullName.put("en", "Team Creator User");
        actingUser = User.builder()
                .email("team.creator@example.com")
                .passwordHash("dummyHash") // Not used for this test directly, but User needs it
                .fullName(actingUserFullName)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();
        actingUser = userRepository.save(actingUser); // Save and get the ID

        sampleTeamName = new HashMap<>();
        sampleTeamName.put("en", "Integration Test Team");
        sampleTeamName.put("fa", "تیم تست یکپارچه‌سازی");

        sampleTeamHandle = "integ_test_team";
        validIntroductoryPostId = "post-intro-for-integ-test"; // Assume this post ID is valid for the test
        // If actual post validation occurs in service:
        // Post introPost = Post.builder().postId(validIntroductoryPostId).title(Map.of("en","Intro Post")).build();
        // postRepository.save(introPost);

        sampleDescription = new HashMap<>();
        sampleDescription.put("en", "An integration test team description.");
    }

    @AfterEach
    void tearDown() {
        testLogger.info("Tearing down data after TeamServiceImplIntegrationTest");
        teamRepository.deleteAll();
        userRepository.deleteAll();
        // postRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: Should create team successfully and persist to DB")
    void createTeam_withValidInputs_shouldPersistTeam() {
        testLogger.info("Test: createTeam_withValidInputs_shouldPersistTeam");

        // Act
        Team createdTeam = teamService.createTeam(
                sampleTeamName,
                sampleTeamHandle,
                validIntroductoryPostId,
                sampleDescription,
                Team.TeamVisibility.PUBLIC,
                true,
                actingUser
        );

        // Assert: Check service response
        assertNotNull(createdTeam);
        assertNotNull(createdTeam.getTeamId());
        assertEquals(sampleTeamName, createdTeam.getTeamName());
        assertEquals(sampleTeamHandle, createdTeam.getTeamHandle());
        assertEquals(actingUser.getUserId(), createdTeam.getCreatorUserId());
        assertNotNull(createdTeam.getMembers());
        assertEquals(1, createdTeam.getMembers().size());
        assertEquals(actingUser.getUserId(), createdTeam.getMembers().get(0).getUserId());
        assertTrue(createdTeam.getMembers().get(0).getRoles().contains("ADMIN"));

        // Assert: Verify directly from the database
        Optional<Team> foundTeamOpt = teamRepository.findById(createdTeam.getTeamId());
        assertTrue(foundTeamOpt.isPresent(), "Team should be found in the database");
        Team foundTeam = foundTeamOpt.get();
        assertEquals(sampleTeamHandle, foundTeam.getTeamHandle());
        assertEquals(sampleTeamName.get("en"), foundTeam.getTeamName().get("en"));
        assertEquals(validIntroductoryPostId, foundTeam.getIntroductoryPostId());

        testLogger.info("Team '{}' persisted successfully with ID: {}", foundTeam.getTeamHandle(), foundTeam.getTeamId());
    }

    @Test
    @DisplayName("Integration Test: Should throw ValidationException when team handle already exists in DB")
    void createTeam_whenHandleExistsInDb_shouldThrowValidationException() {
        testLogger.info("Test: createTeam_whenHandleExistsInDb_shouldThrowValidationException");

        // Arrange: First, save a team with the same handle
        Team existingTeam = Team.builder()
                .teamName(Map.of("en", "Pre-existing Team"))
                .teamHandle(sampleTeamHandle)
                .creatorUserId("another-user-id")
                .introductoryPostId("another-post-id")
                .teamStatus(Team.TeamStatus.ACTIVE)
                .visibility(Team.TeamVisibility.PUBLIC)
                .members(Collections.emptyList())
                .build();
        teamRepository.save(existingTeam);
        testLogger.info("Pre-saved team with handle: {}", sampleTeamHandle);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            teamService.createTeam(
                    sampleTeamName,
                    sampleTeamHandle, // This handle already exists
                    validIntroductoryPostId,
                    sampleDescription,
                    Team.TeamVisibility.PUBLIC,
                    true,
                    actingUser
            );
        });

        assertEquals("Team handle is invalid or already exists: " + sampleTeamHandle, exception.getMessage());

        // Verify that only one team with this handle exists (the one we pre-saved)
        long teamCount = teamRepository.findAll().stream().filter(t -> t.getTeamHandle().equals(sampleTeamHandle)).count();
        assertEquals(1, teamCount, "Only the pre-saved team should exist with this handle");

        testLogger.warn("ValidationException correctly thrown for existing team handle: {}", sampleTeamHandle);
    }

    @Test
    @DisplayName("Integration Test: inviteUserToTeam - should add invited member to team in DB")
    void inviteUserToTeam_whenValid_shouldAddInvitedMember() {
        testLogger.info("Test: inviteUserToTeam_whenValid_shouldAddInvitedMember");
        // Arrange
        // 1. Create and save the team creator (actingUser already created in setUp)
        // 2. Create and save the user to be invited
        Map<String, String> invitedUserFullName = new HashMap<>();
        invitedUserFullName.put("en", "Invited Test User");
        User userToInvite = User.builder()
                .email("invited.user@example.com")
                .passwordHash("dummyHash")
                .fullName(invitedUserFullName)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();
        userToInvite = userRepository.save(userToInvite);

        // 3. Create the team using the service (so creator is already a member and admin)
        Team team = teamService.createTeam(
                sampleTeamName,
                sampleTeamHandle,
                validIntroductoryPostId,
                sampleDescription,
                Team.TeamVisibility.PRIVATE, // Private team, invite only
                true,
                actingUser
        );
        assertNotNull(team.getTeamId());
        String teamId = team.getTeamId();

        // Act: Invite the user
        List<String> rolesForInvitedUser = Collections.singletonList("MEMBER");
        Team updatedTeam = teamService.inviteUserToTeam(teamId, userToInvite.getUserId(), rolesForInvitedUser, actingUser.getUserId());

        // Assert: Check service response
        assertNotNull(updatedTeam.getMembers());
        assertEquals(2, updatedTeam.getMembers().size(), "Team should now have two members/invitations");

        User finalUserToInvite = userToInvite;
        Optional<Team.TeamMember> invitedMemberOpt = updatedTeam.getMembers().stream()
                .filter(m -> m.getUserId().equals(finalUserToInvite.getUserId()))
                .findFirst();
        assertTrue(invitedMemberOpt.isPresent(), "Invited user should be in the members list");
        assertEquals(Team.MemberStatus.INVITED, invitedMemberOpt.get().getStatusInTeam(), "Invited member status should be INVITED");
        assertEquals(rolesForInvitedUser, invitedMemberOpt.get().getRoles(), "Invited member roles should match");

        // Assert: Verify directly from the database
        Team fetchedTeam = teamRepository.findById(teamId)
                .orElseThrow(() -> new AssertionError("Team not found in DB after invite"));

        assertEquals(2, fetchedTeam.getMembers().size());
        User finalUserToInvite1 = userToInvite;
        Optional<Team.TeamMember> fetchedInvitedMemberOpt = fetchedTeam.getMembers().stream()
                .filter(m -> m.getUserId().equals(finalUserToInvite1.getUserId()))
                .findFirst();
        assertTrue(fetchedInvitedMemberOpt.isPresent());
        assertEquals(Team.MemberStatus.INVITED, fetchedInvitedMemberOpt.get().getStatusInTeam());

        testLogger.info("User '{}' successfully invited to team '{}'", userToInvite.getUserId(), teamId);
    }


    // Add more integration tests for TeamServiceImpl:
    // - respondToTeamInvitation (acceptance and decline, check member status in DB)
    // - requestToJoinTeam (for public teams, check PENDING_APPROVAL status)
    // - processMembershipRequest (approve and reject, check member status)
    // - updateTeamMemberRoles (check roles in DB)
    // - removeTeamMember / leaveTeam (check member is removed from DB)
    // - updateTeamInfo (check updated fields in DB)
    // - changeTeamStatus (check status in DB)
    // - Test transactional behavior: if a part of an operation fails, are changes rolled back?
    //   (e.g., if sending a notification fails after adding a member, is the member addition rolled back?)
    //   This requires careful setup of mock failures for dependent services if @Transactional is used.
}