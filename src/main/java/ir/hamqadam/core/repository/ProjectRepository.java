package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {

    /**
     * Finds a project by its unique handle.
     *
     * @param projectHandle The project handle to search for.
     * @return An Optional containing the project if found, or an empty Optional otherwise.
     */
    Optional<Project> findByProjectHandle(String projectHandle);

    /**
     * Checks if a project exists with the given project handle.
     *
     * @param projectHandle The project handle to check.
     * @return True if a project with this handle exists, false otherwise.
     */
    boolean existsByProjectHandle(String projectHandle);

    /**
     * Finds projects by their status.
     *
     * @param status   The project status to filter by.
     * @param pageable Pagination information.
     * @return A page of projects with the specified status.
     */
    Page<Project> findByStatus(Project.ProjectStatus status, Pageable pageable);

    /**
     * Finds projects by their visibility.
     *
     * @param visibility The project visibility to filter by.
     * @param pageable Pagination information.
     * @return A page of projects with the specified visibility.
     */
    Page<Project> findByVisibility(Project.ProjectVisibility visibility, Pageable pageable);

    /**
     * Finds projects created by a specific user or team.
     * Searches within the embedded CreatorInfo object.
     *
     * @param creatorType The type of creator (USER or TEAM).
     * @param creatorId   The ID of the creator.
     * @param pageable    Pagination information.
     * @return A page of projects created by the specified creator.
     */
    Page<Project> findByCreatorInfo_CreatorTypeAndCreatorInfo_CreatorId(
            Project.CreatorType creatorType, String creatorId, Pageable pageable);

    /**
     * Finds projects where a specific team is a managing team.
     *
     * @param teamId   The ID of the managing team.
     * @param pageable Pagination information.
     * @return A page of projects managed by the specified team.
     */
    Page<Project> findByManagingTeamIdsContaining(String teamId, Pageable pageable);

    /**
     * Finds projects where a specific team is a contributing team.
     *
     * @param teamId   The ID of the contributing team.
     * @param pageable Pagination information.
     * @return A page of projects where the specified team is contributing.
     */
    Page<Project> findByContributingTeams_TeamId(String teamId, Pageable pageable);

    /**
     * Finds projects where a specific user is an individual contributor.
     * @param userId The ID of the individual contributor.
     * @param pageable Pagination information.
     * @return A page of projects where the user is an individual contributor.
     */
    Page<Project> findByIndividualContributors_UserId(String userId, Pageable pageable);
}