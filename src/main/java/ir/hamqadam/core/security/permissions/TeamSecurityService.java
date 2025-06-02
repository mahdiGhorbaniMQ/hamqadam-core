package ir.hamqadam.core.security.permissions;

import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.TeamRepository;
import ir.hamqadam.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service("teamSecurityService") // Bean name used in @PreAuthorize, e.g., "@teamSecurityService.isTeamAdmin(...)"
@Transactional(readOnly = true)
public class TeamSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(TeamSecurityService.class);
    private static final String ROLE_ADMIN = "ADMIN"; // Consistent role name

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Autowired
    public TeamSecurityService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    /**
     * Checks if the given user (by username/email) is an admin of the specified team.
     *
     * @param teamId   The ID of the team.
     * @param username The username (email) of the user.
     * @return True if the user is an admin of the team, false otherwise.
     */
    public boolean isTeamAdmin(String teamId, String username) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for permission check: {}", username);
            return false;
        }
        String userId = userOpt.get().getUserId();

        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (teamOpt.isEmpty()) {
            logger.warn("Team not found for permission check: {}", teamId);
            return false; // Or throw ResourceNotFoundException if appropriate for the context
        }

        Team team = teamOpt.get();
        boolean isAdmin = team.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(userId) &&
                        member.getRoles().contains(ROLE_ADMIN) &&
                        member.getStatusInTeam() == Team.MemberStatus.ACTIVE);

        if (!isAdmin) {
            logger.debug("Permission denied: User '{}' is not an admin of team '{}'", username, teamId);
        }
        return isAdmin;
    }

    /**
     * Checks if the user can invite members to the specified team.
     * For Phase 1, this might be restricted to team admins.
     *
     * @param teamId   The ID of the team.
     * @param username The username (email) of the user performing the action.
     * @return True if the user can invite members, false otherwise.
     */
    public boolean canInviteToTeam(String teamId, String username) {
        // For Phase 1, let's assume only team admins can invite.
        // This can be expanded later to include other roles or specific permissions.
        return isTeamAdmin(teamId, username);
    }

    /**
     * Checks if the user is an active member of the team.
     * @param teamId The ID of the team.
     * @param username The username (email) of the user.
     * @return True if the user is an active member, false otherwise.
     */
    public boolean isTeamMember(String teamId, String username) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        String userId = userOpt.get().getUserId();

        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (teamOpt.isEmpty()) {
            return false;
        }
        Team team = teamOpt.get();
        return team.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(userId) &&
                        member.getStatusInTeam() == Team.MemberStatus.ACTIVE);
    }
}