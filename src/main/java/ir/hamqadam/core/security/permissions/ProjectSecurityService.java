package ir.hamqadam.core.security.permissions;

import ir.hamqadam.core.model.Project;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.ProjectRepository;
import ir.hamqadam.core.repository.TeamRepository;
import ir.hamqadam.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service("projectSecurityService")
@Transactional(readOnly = true)
public class ProjectSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectSecurityService.class);
    private static final String ROLE_TEAM_ADMIN = "ADMIN"; // Role within a team

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository; // To check roles in managing teams

    @Autowired
    public ProjectSecurityService(ProjectRepository projectRepository,
                                  UserRepository userRepository,
                                  TeamRepository teamRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    /**
     * Checks if the user can update general project info.
     * Rules for Phase 1: Creator (if user type) or an Admin of any managing team.
     *
     * @param projectId The ID of the project.
     * @param username  The username (email) of the user.
     * @return True if the user can update project info, false otherwise.
     */
    public boolean canUpdateProjectInfo(String projectId, String username) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) return false;
        String userId = userOpt.get().getUserId();

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return false; // Or throw exception if project must exist

        Project project = projectOpt.get();

        // Check if user is the creator (if creator was a USER)
        Project.CreatorInfo creatorInfo = project.getCreatorInfo();
        if (creatorInfo != null && creatorInfo.getCreatorType() == Project.CreatorType.USER &&
                creatorInfo.getCreatorId().equals(userId)) {
            return true;
        }
        // Check if user is the acting user who created it (might be different from formal creator)
        if (creatorInfo != null && creatorInfo.getActingUserId() != null &&
                creatorInfo.getActingUserId().equals(userId)) {
            // This might be too permissive, usually formal creator or managing team admin
            // For now, let's stick to formal creator or managing team admin for updates
        }


        // Check if user is an admin of any of the managing teams
        if (project.getManagingTeamIds() != null) {
            for (String teamId : project.getManagingTeamIds()) {
                Optional<Team> teamOpt = teamRepository.findById(teamId);
                if (teamOpt.isPresent()) {
                    Team team = teamOpt.get();
                    boolean isManagingTeamAdmin = team.getMembers().stream()
                            .anyMatch(member -> member.getUserId().equals(userId) &&
                                    member.getRoles().contains(ROLE_TEAM_ADMIN) &&
                                    member.getStatusInTeam() == Team.MemberStatus.ACTIVE);
                    if (isManagingTeamAdmin) return true;
                }
            }
        }
        logger.debug("Permission denied: User '{}' cannot update info for project '{}'", username, projectId);
        return false;
    }

    /**
     * Checks if the user can change the project status.
     * Similar rules to updating info for Phase 1.
     */
    public boolean canChangeProjectStatus(String projectId, String username) {
        return canUpdateProjectInfo(projectId, username); // Reusing logic for simplicity in Phase 1
    }

    /**
     * Checks if the user can manage teams associated with the project.
     * Similar rules to updating info for Phase 1.
     */
    public boolean canManageProjectTeams(String projectId, String username) {
        return canUpdateProjectInfo(projectId, username); // Reusing logic
    }

    /**
     * Checks if the user can manage individual contributors of the project.
     * Similar rules to updating info for Phase 1.
     */
    public boolean canManageProjectContributors(String projectId, String username) {
        return canUpdateProjectInfo(projectId, username); // Reusing logic
    }

    /**
     * Checks if a user can view a specific project.
     * Public projects are viewable by anyone. Private projects are viewable by members.
     * @param projectId The project ID.
     * @param username The username (email) of the user. Null if unauthenticated.
     * @return True if user can view, false otherwise.
     */
    public boolean canViewProject(String projectId, String username) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return false; // Or throw ResourceNotFound
        }
        Project project = projectOpt.get();

        if (project.getVisibility() == Project.ProjectVisibility.PUBLIC) {
            return true;
        }

        // For private projects, user must be authenticated and involved
        if (username == null) {
            return false; // Unauthenticated user cannot see private projects
        }

        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        String userId = userOpt.get().getUserId();

        // Is creator?
        Project.CreatorInfo creatorInfo = project.getCreatorInfo();
        if (creatorInfo != null && creatorInfo.getCreatorType() == Project.CreatorType.USER &&
                creatorInfo.getCreatorId().equals(userId)) {
            return true;
        }
        if (creatorInfo != null && creatorInfo.getActingUserId() != null &&
                creatorInfo.getActingUserId().equals(userId)) {
            // This is more like 'who clicked the button', not necessarily formal creator role.
            // Let's consider formal involvement for viewing private projects.
        }

        // Is member of managing team?
        if (project.getManagingTeamIds() != null) {
            for (String teamId : project.getManagingTeamIds()) {
                Optional<Team> teamOpt = teamRepository.findById(teamId);
                if (teamOpt.isPresent() && teamOpt.get().getMembers().stream()
                        .anyMatch(m -> m.getUserId().equals(userId) && m.getStatusInTeam() == Team.MemberStatus.ACTIVE)) {
                    return true;
                }
            }
        }
        // Is member of contributing team?
        if (project.getContributingTeams() != null) {
            for (Project.ContributingTeamInfo cTeamInfo : project.getContributingTeams()) {
                Optional<Team> teamOpt = teamRepository.findById(cTeamInfo.getTeamId());
                if (teamOpt.isPresent() && teamOpt.get().getMembers().stream()
                        .anyMatch(m -> m.getUserId().equals(userId) && m.getStatusInTeam() == Team.MemberStatus.ACTIVE)) {
                    return true;
                }
            }
        }
        // Is an individual contributor?
        if (project.getIndividualContributors() != null && project.getIndividualContributors().stream()
                .anyMatch(ic -> ic.getUserId().equals(userId))) {
            return true;
        }

        logger.debug("Permission denied: User '{}' cannot view private project '{}'", username, projectId);
        return false;
    }
}