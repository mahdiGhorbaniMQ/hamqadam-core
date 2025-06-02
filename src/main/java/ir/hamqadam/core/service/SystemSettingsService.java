package ir.hamqadam.core.service;

import ir.hamqadam.core.controller.dto.admin.SystemSettingDTO; // Using the DTO for updates
import ir.hamqadam.core.model.SystemSetting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SystemSettingsService {

    /**
     * Retrieves a specific system setting by its key.
     *
     * @param key The key of the setting.
     * @return An Optional containing the SystemSetting if found.
     */
    Optional<SystemSetting> getSetting(String key);

    /**
     * Retrieves the string value of a setting, with a default if not found.
     * @param key The key of the setting.
     * @param defaultValue The value to return if the setting is not found.
     * @return The setting value or the default.
     */
    String getString(String key, String defaultValue);

    /**
     * Retrieves the integer value of a setting, with a default if not found or not an integer.
     * @param key The key of the setting.
     * @param defaultValue The value to return if the setting is not found or invalid.
     * @return The setting value or the default.
     */
    int getInt(String key, int defaultValue);

    /**
     * Retrieves the boolean value of a setting, with a default if not found.
     * Recognizes "true", "yes", "1" as true (case-insensitive).
     * @param key The key of the setting.
     * @param defaultValue The value to return if the setting is not found.
     * @return The setting value or the default.
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Retrieves all system settings.
     *
     * @return A list of all SystemSettings.
     */
    List<SystemSetting> getAllSettings();

    /**
     * Creates or updates a single system setting.
     *
     * @param key         The key of the setting.
     * @param value       The new value for the setting.
     * @param description (Optional) i18n description for the setting.
     * @param typeHint    (Optional) type hint for the value.
     * @return The created or updated SystemSetting.
     */
    SystemSetting saveSetting(String key, String value, Map<String, String> description, String typeHint);

    /**
     * Updates a list of system settings.
     * This typically comes from the AdminController.
     * @param settingsToUpdate List of SystemSettingDTOs.
     * @return List of updated SystemSetting objects.
     */
    List<SystemSetting> updateSettings(List<SystemSettingDTO> settingsToUpdate);


    /**
     * Initializes default system settings if they don't already exist.
     * This could be called on application startup.
     */
    void initializeDefaultSettings();
}