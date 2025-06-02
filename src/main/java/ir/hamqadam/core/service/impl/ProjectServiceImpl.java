package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.exception.UnauthorizedException;
import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.Project;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.model.Team;
// import ir.hamqadam.core.model.Post; // If validating descriptivePostId existence
import ir.hamqadam.core.repository.ProjectRepository;
import ir.hamqadam.core.repository.UserRepository;
import ir.hamqadam.core.repository.TeamRepository;
// import ir.hamqadam.core.repository.PostRepository; // If validating
import ir.hamqadam.core.service.ProjectService;
// import ir.hamqadam.core.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    // private final PostRepository postRepository; // To validate descriptivePostId
    // private final NotificationService notificationService;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserRepository userRepository,
                              TeamRepository teamRepository
            /*, PostRepository postRepository, NotificationService notificationService */) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        // this.postRepository = postRepository;
        // this.notificationService = notificationService;
    }

    @Override
    public Project createProject(Map<String, String> projectName,
                                 String projectHandle,
                                 String descriptivePostId,
                                 Project.CreatorInfo creatorInfo,
                                 Project.ProjectVisibility visibility,
                                 Project.ProjectStatus initialStatus,
                                 List<String> managingTeamIds,
                                 User actingUser) {

        if (StringUtils.hasText(projectHandle) && projectRepository.existsByProjectHandle(projectHandle)) {
            throw new ValidationException("Project handle already exists: " + projectHandle);
        }
        if (!StringUtils.hasText(descriptivePostId)) {
            throw new ValidationException("Descriptive Post ID is required for creating a project.");
        }
        // Optional: Validate existence of descriptivePostId
        // postRepository.findById(descriptivePostId)
        //        .orElseThrow(() -> new ResourceNotFoundException("Post", "ID", descriptivePostId));

        // Validate creator
        if (creatorInfo.getCreatorType() == Project.CreatorType.USER) {
            userRepository.findById(creatorInfo.getCreatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("User (creator)", "ID", creatorInfo.getCreatorId()));
        } else if (creatorInfo.getCreatorType() == Project.CreatorType.TEAM) {
            teamRepository.findById(creatorInfo.getCreatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team (creator)", "ID", creatorInfo.getCreatorId()));
        }

        // Validate managing teams if provided
        if (managingTeamIds != null) {
            for (String teamId : managingTeamIds) {
                teamRepository.findById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException("Managing Team", "ID", teamId));
            }
        }


        Project newProject = Project.builder()
                .projectName(projectName)
                .projectHandle(StringUtils.hasText(projectHandle) ? projectHandle : null) // Store null if blank
                .descriptivePostId(descriptivePostId)
                .creatorInfo(Project.CreatorInfo.builder() // Ensure acting user is set
                        .creatorType(creatorInfo.getCreatorType())
                        .creatorId(creatorInfo.getCreatorId())
                        .actingUserId(actingUser.getUserId())
                        .build())
                .visibility(visibility)
                .status(initialStatus != null ? initialStatus : Project.ProjectStatus.IDEA_PROPOSAL)
                .managingTeamIds(managingTeamIds != null ? new ArrayList<>(managingTeamIds) : new ArrayList<>())
                .contributingTeams(new ArrayList<>())
                .individualContributors(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        logger.info("Creating new project '{}' by acting user '{}'", newProject.getProjectName(), actingUser.getUserId());
        return projectRepository.save(newProject);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findProjectById(String projectId) {
        return projectRepository.findById(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findProjectByHandle(String projectHandle) {
        if (!StringUtils.hasText(projectHandle)) {
            return Optional.empty();
        }
        return projectRepository.findByProjectHandle(projectHandle);
    }

    @Override
    @PreAuthorize("@projectSecurityService.canUpdateProjectInfo(#projectId, principal.username)")
    public Project updateProjectInfo(String projectId,
                                     Map<String, String> projectName,
                                     Project.ProjectVisibility visibility,
                                     Map<String, String> projectGoals,
                                     Map<String, String> projectScope,
                                     String actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "projectId", projectId));

        // checkProjectManagementPermission(project, actingUserId); // Or use @PreAuthorize

        if (projectName != null) project.setProjectName(projectName);
        if (visibility != null) project.setVisibility(visibility);
        if (projectGoals != null) project.setProjectGoals(projectGoals);
        if (projectScope != null) project.setProjectScope(projectScope);
        // Update other fields...

        project.setUpdatedAt(LocalDateTime.now());
        logger.info("Project '{}' info updated by user '{}'", projectId, actingUserId);
        return projectRepository.save(project);
    }

    private void checkProjectManagementPermission(Project project, String actingUserId) {
        // Simplified check: creator or member of a managing team
        boolean canManage = false;
        if (project.getCreatorInfo() != null && project.getCreatorInfo().getActingUserId().equals(actingUserId)) {
            canManage = true;
        } else if (project.getCreatorInfo() != null && project.getCreatorInfo().getCreatorType() == Project.CreatorType.USER && project.getCreatorInfo().getCreatorId().equals(actingUserId)) {
            canManage = true;
        }

        if (!canManage && project.getManagingTeamIds() != null) {
            for (String teamId : project.getManagingTeamIds()) {
                Team team = teamRepository.findById(teamId).orElse(null);
                if (team != null && team.getMembers().stream().anyMatch(m -> m.getUserId().equals(actingUserId) && m.getRoles().contains("ADMIN"))) { // Assuming team admin can manage project
                    canManage = true;
                    break;
                }
            }
        }
        if (!canManage) {
            throw new UnauthorizedException("User " + actingUserId + " is not authorized to manage project " + project.getProjectId());
        }
    }


    @Override
    @PreAuthorize("@projectSecurityService.canChangeProjectStatus(#projectId, principal.username)")
    public Project changeProjectStatus(String projectId, Project.ProjectStatus newStatus, String actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "projectId", projectId));

        // checkProjectManagementPermission(project, actingUserId);

        // Add validation for valid status transitions if needed (Phase 1: simplified)
        // e.g., cannot go from COMPLETED back to IN_PROGRESS without specific logic

        project.setStatus(newStatus);
        project.setUpdatedAt(LocalDateTime.now());
        logger.info("Project '{}' status changed to {} by user '{}'", projectId, newStatus, actingUserId);
        // notificationService.notifyProjectStakeholders(projectId, "Project status changed to " + newStatus);
        return projectRepository.save(project);
    }

    @Override
    @PreAuthorize("@projectSecurityService.canManageProjectTeams(#projectId, principal.username)")
    public Project addTeamToProject(String projectId, String teamId, boolean isManaging, String roleInProject, String actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "projectId", projectId));
        teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "teamId", teamId));

        // checkProjectManagementPermission(project, actingUserId);

        if (isManaging) {
            if (project.getManagingTeamIds() == null) project.setManagingTeamIds(new ArrayList<>());
            if (!project.getManagingTeamIds().contains(teamId)) {
                project.getManagingTeamIds().add(teamId);
            }
        } else {
            if (project.getContributingTeams() == null) project.setContributingTeams(new ArrayList<>());
            boolean alreadyContributing = project.getContributingTeams().stream().anyMatch(ct -> ct.getTeamId().equals(teamId));
            if (!alreadyContributing) {
                project.getContributingTeams().add(new Project.ContributingTeamInfo(teamId, roleInProject));
            } else {
                throw new ValidationException("Team " + teamId + " is already a contributing team.");
            }
        }
        project.setUpdatedAt(LocalDateTime.now());
        logger.info("Team '{}' added to project '{}' by user '{}'", teamId, projectId, actingUserId);
        return projectRepository.save(project);
    }

    @Override
    @PreAuthorize("@projectSecurityService.canManageProjectTeams(#projectId, principal.username)")
    public Project removeTeamFromProject(String projectId, String teamId, String actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "projectId", projectId));

        // checkProjectManagementPermission(project, actingUserId);

        boolean removed = false;
        if (project.getManagingTeamIds() != null) {
            removed = project.getManagingTeamIds().remove(teamId);
        }
        if (!removed && project.getContributingTeams() != null) {
            removed = project.getContributingTeams().removeIf(ct -> ct.getTeamId().equals(teamId));
        }

        if (!removed) {
            throw new ResourceNotFoundException("Team " + teamId + " not found in project " + projectId + " participants.");
        }

        project.setUpdatedAt(LocalDateTime.now());
        logger.info("Team '{}' removed from project '{}' by user '{}'", teamId, projectId, actingUserId);
        return projectRepository.save(project);
    }

    @Override
    @PreAuthorize("@projectSecurityService.canManageProjectContributors(#projectId, principal.username)")
    public Project addIndividualContributorToProject(String projectId,
                                                     String contributorUserId,
                                                     String roleInProject,
                                                     Map<String, String> contributionDescription,
                                                     String actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "projectId", projectId));
        userRepository.findById(contributorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User (contributor)", "ID", contributorUserId));

        // checkProjectManagementPermission(project, actingUserId);

        if (project.getIndividualContributors() == null) {
            project.setIndividualContributors(new ArrayList<>());
        }
        boolean alreadyContributor = project.getIndividualContributors().stream()
                .anyMatch(ic -> ic.getUserId().equals(contributorUserId));
        if (alreadyContributor) {
            throw new ValidationException("User " + contributorUserId + " is already an individual contributor.");
        }

        project.getIndividualContributors().add(
                new Project.IndividualContributorInfo(contributorUserId, roleInProject, contributionDescription)
        );
        project.setUpdatedAt(LocalDateTime.now());
        logger.info("User '{}' added as individual contributor to project '{}' by user '{}'", contributorUserId, projectId, actingUserId);
        return projectRepository.save(project);
    }

    @Override
    @PreAuthorize("@projectSecurityService.canManageProjectContributors(#projectId, principal.username)")
    public Project removeIndividualContributorFromProject(String projectId, String contributorUserId, String actingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "projectId", projectId));

        // checkProjectManagementPermission(project, actingUserId);

        if (project.getIndividualContributors() == null) {
            throw new ResourceNotFoundException("Individual contributor " + contributorUserId + " not found in project " + projectId);
        }
        boolean removed = project.getIndividualContributors().removeIf(ic -> ic.getUserId().equals(contributorUserId));
        if (!removed) {
            throw new ResourceNotFoundException("Individual contributor " + contributorUserId + " not found in project " + projectId);
        }
        project.setUpdatedAt(LocalDateTime.now());
        logger.info("Individual contributor '{}' removed from project '{}' by user '{}'", contributorUserId, projectId, actingUserId);
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findProjectsByManagingTeam(String teamId, Pageable pageable) {
        return projectRepository.findByManagingTeamIdsContaining(teamId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findProjectsByContributingTeam(String teamId, Pageable pageable) {
        return projectRepository.findByContributingTeams_TeamId(teamId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findProjectsByIndividualContributor(String userId, Pageable pageable) {
        return projectRepository.findByIndividualContributors_UserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findProjectsByCreator(Project.CreatorType creatorType, String creatorId, Pageable pageable) {
        return projectRepository.findByCreatorInfo_CreatorTypeAndCreatorInfo_CreatorId(creatorType, creatorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> searchPublicProjects(String query, Pageable pageable) {
        // Basic implementation for Phase 1
        if (!StringUtils.hasText(query)) {
            return projectRepository.findByVisibility(Project.ProjectVisibility.PUBLIC, pageable);
        }
        // Requires a method in ProjectRepository like:
        // Page<Project> findByVisibilityAndProjectNameContainingIgnoreCase(ProjectVisibility visibility, String nameQuery, Pageable pageable);
        // Or a more complex text search.
        logger.warn("SearchPublicProjects basic implementation used. Query: {}", query);
        return projectRepository.findByVisibility(Project.ProjectVisibility.PUBLIC, pageable); // Placeholder
    }

    // Implementation for simplified task management (modifying Project entity directly)
    // would go here if those methods were activated in the interface.
    // For example:
    // @Override
    // @PreAuthorize("@projectSecurityService.canManageProjectTasks(#projectId, principal.username)")
    // public Project createTaskInProject(...) { ... project.getSimpleTasks().add(newTask); ... }
}