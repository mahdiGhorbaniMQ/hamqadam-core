package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.SystemSetting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends MongoRepository<SystemSetting, String> {
    // String is the type of the ID (which is 'key')

    /**
     * Finds a system setting by its key.
     *
     * @param key The setting key.
     * @return An Optional containing the SystemSetting if found.
     */
    Optional<SystemSetting> findByKey(String key);
}