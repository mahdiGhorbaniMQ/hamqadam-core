package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends MongoRepository<Team, String> {

    /**
     * Finds a team by its unique handle.
     *
     * @param teamHandle The team handle to search for.
     * @return An Optional containing the team if found, or an empty Optional otherwise.
     */
    Optional<Team> findByTeamHandle(String teamHandle);

    /**
     * Checks if a team exists with the given team handle.
     *
     * @param teamHandle The team handle to check.
     * @return True if a team with this handle exists, false otherwise.
     */
    boolean existsByTeamHandle(String teamHandle);

    /**
     * Finds teams created by a specific user.
     *
     * @param creatorUserId The ID of the user who created the teams.
     * @param pageable      Pagination information.
     * @return A page of teams created by the specified user.
     */
    Page<Team> findByCreatorUserId(String creatorUserId, Pageable pageable);

    /**
     * Finds teams by their visibility status.
     *
     * @param visibility The visibility status to filter by.
     * @param pageable   Pagination information.
     * @return A page of teams with the specified visibility.
     */
    Page<Team> findByVisibility(Team.TeamVisibility visibility, Pageable pageable);

    /**
     * Finds teams where a specific user is a member.
     * This query will search within the 'members' array for an element where 'userId' matches.
     *
     * @param userId The ID of the user.
     * @param pageable Pagination information.
     * @return A page of teams where the user is a member.
     */
    Page<Team> findByMembers_UserId(String userId, Pageable pageable);

    /**
     * Finds teams whose name (in any language) contains the given search term, ignoring case.
     * Similar to UserRepository, searching i18n map values directly with derived queries is complex.
     * A custom @Query with regex or a text index on the i18n map values is recommended.
     *
     * @param nameQuery The search term for the team name.
     * @param pageable Pagination information.
     * @return A page of teams matching the name query.
     */
    // Page<Team> findByTeamNameContainingIgnoreCase(String nameQuery, Pageable pageable); // Placeholder
}