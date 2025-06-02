package ir.hamqadam.core.service;

import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TeamService {

    /**
     * Creates a new team.
     * The introductory post should be created separately and its ID provided.
     * The actingUser will be the creator and initial admin of the team.
     *
     * @param teamName             i18n map of the team name.
     * @param teamHandle           Unique handle for the team.
     * @param introductoryPostId   ID of the introductory post.
     * @param description          i18n map of the team description.
     * @param visibility           Visibility of the team.
     * @param membershipApprovalRequired Whether membership requires approval for public teams.
     * @param actingUser           The user creating the team.
     * @return The created Team object.
     * @throws ir.hamqadam.core.exception.ValidationException if teamHandle already exists or data is invalid.
     */
    Team createTeam(Map<String, String> teamName,
                    String teamHandle,
                    String introductoryPostId,
                    Map<String, String> description,
                    Team.TeamVisibility visibility,
                    boolean membershipApprovalRequired,
                    User actingUser);

    /**
     * Finds a team by its ID.
     *
     * @param teamId The ID of the team.
     * @return An Optional containing the team if found.
     */
    Optional<Team> findTeamById(String teamId);

    /**
     * Finds a team by its unique handle.
     *
     * @param teamHandle The handle of the team.
     * @return An Optional containing the team if found.
     */
    Optional<Team> findTeamByHandle(String teamHandle);

    /**
     * Updates an existing team's information.
     *
     * @param teamId            The ID of the team to update.
     * @param updatedTeamFields A Team object or DTO with fields to update.
     * (For this example, individual updatable fields are listed)
     * @param actingUserId      ID of the user performing the update (for permission checks).
     * @return The updated Team object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Team updateTeamInfo(String teamId,
                        Map<String, String> teamName, // i18n
                        Map<String, String> description, // i18n
                        String profilePictureUrl,
                        String coverPictureUrl,
                        Team.TeamVisibility visibility,
                        boolean membershipApprovalRequired,
                        String actingUserId);

    /**
     * Invites a user to join a team.
     *
     * @param teamId          The ID of the team.
     * @param userIdToInvite  The ID of the user to invite.
     * @param rolesForUser    List of roles (strings for Phase 1) to assign initially.
     * @param actingUserId    ID of the user performing the invitation.
     * @return The updated Team object or a confirmation message.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team or userToInvite not found.
     * @throws ir.hamqadam.core.exception.ValidationException if user is already a member or invited.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Team inviteUserToTeam(String teamId, String userIdToInvite, List<String> rolesForUser, String actingUserId);

    /**
     * Allows a user to respond to a team invitation.
     *
     * @param teamId       The ID of the team.
     * @param invitedUserId The ID of the user who was invited.
     * @param accept       True to accept, false to decline.
     * @return The updated Team object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team or invitation not found.
     * @throws ir.hamqadam.core.exception.ValidationException if user was not invited or already responded.
     */
    Team respondToTeamInvitation(String teamId, String invitedUserId, boolean accept);

    /**
     * Allows a user to request membership in a public team.
     *
     * @param teamId        The ID of the public team.
     * @param requestingUserId The ID of the user requesting to join.
     * @return The updated Team object or a confirmation.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team not found.
     * @throws ir.hamqadam.core.exception.ValidationException if team is not public, or user is already member/requested.
     */
    Team requestToJoinTeam(String teamId, String requestingUserId);

    /**
     * Allows a team admin to approve or reject a membership request.
     *
     * @param teamId          The ID of the team.
     * @param userIdToProcess The ID of the user whose request is being processed.
     * @param approve         True to approve, false to reject.
     * @param actingUserId    ID of the admin performing the action.
     * @return The updated Team object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team or request not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Team processMembershipRequest(String teamId, String userIdToProcess, boolean approve, String actingUserId);

    /**
     * Updates a member's roles within a team.
     *
     * @param teamId         The ID of the team.
     * @param memberUserId   The ID of the member whose roles are to be updated.
     * @param newRoles       The new list of roles for the member.
     * @param actingUserId   ID of the admin performing the action.
     * @return The updated Team object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team or member not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Team updateTeamMemberRoles(String teamId, String memberUserId, List<String> newRoles, String actingUserId);

    /**
     * Removes a member from a team.
     *
     * @param teamId       The ID of the team.
     * @param memberUserId The ID of the member to remove.
     * @param actingUserId ID of the admin performing the action.
     * @return The updated Team object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team or member not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     * @throws ir.hamqadam.core.exception.ValidationException if trying to remove the last admin etc.
     */
    Team removeTeamMember(String teamId, String memberUserId, String actingUserId);

    /**
     * Allows a user to leave a team.
     *
     * @param teamId      The ID of the team.
     * @param memberUserId The ID of the user leaving the team.
     * @return The updated Team object or confirmation.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if team or member not found.
     * @throws ir.hamqadam.core.exception.ValidationException if user is last admin and cannot leave.
     */
    Team leaveTeam(String teamId, String memberUserId);

    /**
     * Changes the status of a team (e.g., archive, disband).
     *
     * @param teamId The ID of the team.
     * @param newStatus The new status for the team.
     * @param actingUserId ID of the user performing the action (must be admin).
     * @return The updated Team object.
     */
    Team changeTeamStatus(String teamId, Team.TeamStatus newStatus, String actingUserId);

    /**
     * Finds all teams with pagination.
     * For admins or public listings.
     *
     * @param pageable Pagination information.
     * @return A Page of Teams.
     */
    Page<Team> findAllTeams(Pageable pageable);

    /**
     * Finds teams where a user is a member.
     * @param userId The ID of the user.
     * @param pageable Pagination information.
     * @return A Page of Teams.
     */
    Page<Team> findTeamsByMemberUserId(String userId, Pageable pageable);

    /**
     * Searches for public teams based on a query (e.g., name, description).
     * @param query The search query.
     * @param pageable Pagination information.
     * @return A Page of matching public Teams.
     */
    Page<Team> searchPublicTeams(String query, Pageable pageable);

    // Sub-team creation could be a separate service or part of more advanced features.
    // For Phase 1, creating a team and manually linking its parent_team_id might suffice if needed.
}