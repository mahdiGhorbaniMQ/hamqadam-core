package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by their email address.
     *
     * @param email The email address to search for.
     * @return An Optional containing the user if found, or an empty Optional otherwise.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their Telegram ID.
     *
     * @param telegramId The Telegram ID to search for.
     * @return An Optional containing the user if found, or an empty Optional otherwise.
     */
    Optional<User> findByTelegramId(String telegramId);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email The email to check.
     * @return True if a user with this email exists, false otherwise.
     */
    Boolean existsByEmail(String email);

    /**
     * Checks if a user exists with the given Telegram ID.
     *
     * @param telegramId The Telegram ID to check.
     * @return True if a user with this Telegram ID exists, false otherwise.
     */
    Boolean existsByTelegramId(String telegramId);

    /**
     * Finds users whose full name (in any language) contains the given search term, ignoring case.
     * This requires your i18n map keys to be consistent or a more complex query/text index.
     * For a simple search across i18n names, you might need a custom @Query or specific indexing on the i18n map.
     * This is a simplified example.
     *
     * @param nameQuery The search term for the full name.
     * @param pageable  Pagination information.
     * @return A page of users matching the name query.
     */
    // For i18n fields, derived queries are tricky. A @Query might be better.
    // Example of what you might want (conceptual, direct translation might not work perfectly for map values):
    // Page<User> findByFullNameContainingIgnoreCase(String nameQuery, Pageable pageable);
    // A more robust way would be to use @Query or a text index.
    // For now, this is a placeholder for search functionality.

    /**
     * Finds users by their account status.
     *
     * @param accountStatus The account status to filter by.
     * @param pageable      Pagination information.
     * @return A page of users with the specified account status.
     */
    Page<User> findByAccountStatus(User.AccountStatus accountStatus, Pageable pageable);

    /**
     * Finds users by a skill.
     * As skills is a list, this will find users where the skills list contains the given skill.
     * @param skill The skill to search for.
     * @param pageable Pagination information.
     * @return A page of users having the specified skill.
     */
    Page<User> findBySkillsContainingIgnoreCase(String skill, Pageable pageable);
}