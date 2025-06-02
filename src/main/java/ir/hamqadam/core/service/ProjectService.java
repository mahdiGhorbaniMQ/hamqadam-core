package ir.hamqadam.core.service;

import ir.hamqadam.core.model.Project;
import ir.hamqadam.core.model.User; // Assuming needed for actingUser or contributor info
import ir.hamqadam.core.model.Team;  // Assuming needed for team info
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProjectService {

    /**
     * Creates a new project.
     * The descriptive post should be created separately and its ID provided.
     *
     * @param projectName         i18n map of the project name.
     * @param projectHandle       Unique handle for the project (optional).
     * @param descriptivePostId   ID of the descriptive post.
     * @param creatorInfo         Information about the creator (User or Team).
     * @param visibility          Visibility of the project.
     * @param initialStatus       Initial status of the project (from Project.ProjectStatus enum).
     * @param managingTeamIds     List of IDs of teams initially managing the project.
     * @param actingUser          The user performing the creation action.
     * @return The created Project object.
     * @throws ir.hamqadam.core.exception.ValidationException if data is invalid (e.g., handle exists).
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if creator entity or post not found.
     */
    Project createProject(Map<String, String> projectName,
                          String projectHandle,
                          String descriptivePostId,
                          Project.CreatorInfo creatorInfo,
                          Project.ProjectVisibility visibility,
                          Project.ProjectStatus initialStatus,
                          List<String> managingTeamIds,
                          User actingUser);

    /**
     * Finds a project by its ID.
     *
     * @param projectId The ID of the project.
     * @return An Optional containing the project if found.
     */
    Optional<Project> findProjectById(String projectId);

    /**
     * Finds a project by its unique handle.
     *
     * @param projectHandle The handle of the project.
     * @return An Optional containing the project if found.
     */
    Optional<Project> findProjectByHandle(String projectHandle);

    /**
     * Updates an existing project's information.
     *
     * @param projectId         The ID of the project to update.
     * @param updatedFields     A map or DTO containing fields to update (e.g., name, description, status, visibility).
     * For this example, specific fields are listed.
     * @param actingUserId      ID of the user performing the update (for permission checks).
     * @return The updated Project object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if project not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Project updateProjectInfo(String projectId,
                              Map<String, String> projectName, // i18n
                              Project.ProjectVisibility visibility,
                              Map<String, String> projectGoals, // i18n
                              Map<String, String> projectScope, // i18n
                              // ... other updatable general fields
                              String actingUserId);

    /**
     * Changes the status of a project (Phase 1 fixed workflow).
     *
     * @param projectId    The ID of the project.
     * @param newStatus    The new status for the project.
     * @param actingUserId ID of the user performing the action.
     * @return The updated Project object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if project not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     * @throws ir.hamqadam.core.exception.ValidationException if status transition is invalid.
     */
    Project changeProjectStatus(String projectId, Project.ProjectStatus newStatus, String actingUserId);

    /**
     * Adds a team as a managing or contributing team to a project.
     *
     * @param projectId    The ID of the project.
     * @param teamId       The ID of the team to add.
     * @param isManaging   True if the team is a managing team, false if contributing.
     * @param roleInProject Optional role description for the team in the project.
     * @param actingUserId ID of the user performing the action.
     * @return The updated Project object.
     */
    Project addTeamToProject(String projectId, String teamId, boolean isManaging, String roleInProject, String actingUserId);

    /**
     * Removes a team from a project.
     *
     * @param projectId    The ID of the project.
     * @param teamId       The ID of the team to remove.
     * @param actingUserId ID of the user performing the action.
     * @return The updated Project object.
     */
    Project removeTeamFromProject(String projectId, String teamId, String actingUserId);

    /**
     * Adds an individual contributor to a project.
     *
     * @param projectId                 The ID of the project.
     * @param contributorUserId         The ID of the user to add.
     * @param roleInProject             Role of the individual in the project.
     * @param contributionDescription   i18n map describing their contribution.
     * @param actingUserId              ID of the user performing the action.
     * @return The updated Project object.
     */
    Project addIndividualContributorToProject(String projectId,
                                              String contributorUserId,
                                              String roleInProject,
                                              Map<String, String> contributionDescription,
                                              String actingUserId);

    /**
     * Removes an individual contributor from a project.
     *
     * @param projectId         The ID of the project.
     * @param contributorUserId The ID of the user to remove.
     * @param actingUserId      ID of the user performing the action.
     * @return The updated Project object.
     */
    Project removeIndividualContributorFromProject(String projectId, String contributorUserId, String actingUserId);

    /**
     * Finds projects managed by a specific team.
     *
     * @param teamId   The ID of the managing team.
     * @param pageable Pagination information.
     * @return A Page of Projects.
     */
    Page<Project> findProjectsByManagingTeam(String teamId, Pageable pageable);

    /**
     * Finds projects where a specific team is contributing.
     *
     * @param teamId   The ID of the contributing team.
     * @param pageable Pagination information.
     * @return A Page of Projects.
     */
    Page<Project> findProjectsByContributingTeam(String teamId, Pageable pageable);

    /**
     * Finds projects where a specific user is an individual contributor.
     * @param userId The ID of the user.
     * @param pageable Pagination information.
     * @return A Page of Projects.
     */
    Page<Project> findProjectsByIndividualContributor(String userId, Pageable pageable);

    /**
     * Finds projects created by a specific user or team.
     * @param creatorType The type of creator (USER or TEAM).
     * @param creatorId The ID of the creator.
     * @param pageable Pagination information.
     * @return A Page of Projects.
     */
    Page<Project> findProjectsByCreator(Project.CreatorType creatorType, String creatorId, Pageable pageable);

    /**
     * Searches for public projects based on a query (e.g., name, description).
     * @param query The search query.
     * @param pageable Pagination information.
     * @return A Page of matching public Projects.
     */
    Page<Project> searchPublicProjects(String query, Pageable pageable);

    // --- Simplified Task Management for Phase 1 (as discussed for API, services expose this) ---
    // In a more complex system, Task would be its own entity and service.

    /**
     * Creates a simple task description within a project (Phase 1).
     * This might append to a list of task descriptions in the Project model or link to a simple Post.
     * For this example, let's assume it adds a simple task object to a list in Project.
     *
     * @param projectId       The ID of the project.
     * @param taskTitle       i18n map for the task title.
     * @param taskDescription i18n map for the task description.
     * @param assigneeUserId  Optional ID of the user assigned to the task.
     * @param actingUserId    ID of the user creating the task.
     * @return The updated Project object with the new task info.
     */
    // Project createTaskInProject(String projectId,
    //                             Map<String, String> taskTitle,
    //                             Map<String, String> taskDescription,
    //                             String assigneeUserId,
    //                             String actingUserId);

    /**
     * Updates the status of a simple task within a project (Phase 1).
     *
     * @param projectId    The ID of the project.
     * @param taskId       The identifier for the task within the project.
     * @param newStatus    The new status for the task.
     * @param actingUserId ID of the user updating the task.
     * @return The updated Project object.
     */
    // Project updateTaskStatusInProject(String projectId, String taskId, String newStatus, String actingUserId);
}