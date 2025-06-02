package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.controller.dto.admin.SystemSettingDTO;
import ir.hamqadam.core.model.SystemSetting;
import ir.hamqadam.core.repository.SystemSettingRepository;
import ir.hamqadam.core.service.SystemSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value; // For reading from application.properties

import jakarta.annotation.PostConstruct; // For initializeDefaultSettings
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap; // For default settings

@Service
@Transactional
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsServiceImpl.class);

    private final SystemSettingRepository systemSettingRepository;

    // You can inject values from application.properties to serve as initial defaults
    // or fallback values if the database setting is missing.
    @Value("${hamqadam.default.site_name:Hamqadam Platform}")
    private String defaultSiteName;

    @Value("${hamqadam.default.max_team_members:50}")
    private int defaultMaxTeamMembers;

    @Value("${hamqadam.default.allow_new_registrations:true}")
    private boolean defaultAllowNewRegistrations;


    @Autowired
    public SystemSettingsServiceImpl(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @PostConstruct
    public void init() {
        initializeDefaultSettings();
    }

    @Override
    public void initializeDefaultSettings() {
        logger.info("Initializing default system settings...");
        // Example default settings
        Map<String, String> defaultSiteNameDesc = new HashMap<>();
        defaultSiteNameDesc.put("en", "The public name of the platform.");
        defaultSiteNameDesc.put("fa", "نام عمومی پلتفرم.");
        saveSettingIfNotExists("site_name", defaultSiteName, defaultSiteNameDesc, "STRING");

        Map<String, String> maxTeamMembersDesc = new HashMap<>();
        maxTeamMembersDesc.put("en", "Maximum members allowed per team.");
        maxTeamMembersDesc.put("fa", "حداکثر تعداد اعضای مجاز برای هر تیم.");
        saveSettingIfNotExists("max_team_members", String.valueOf(defaultMaxTeamMembers), maxTeamMembersDesc, "INTEGER");

        Map<String, String> allowNewRegDesc = new HashMap<>();
        allowNewRegDesc.put("en", "Whether new user registrations are open.");
        allowNewRegDesc.put("fa", "آیا ثبت‌نام کاربران جدید باز است.");
        saveSettingIfNotExists("allow_new_registrations", String.valueOf(defaultAllowNewRegistrations), allowNewRegDesc, "BOOLEAN");

        logger.info("Default system settings initialization complete.");
    }

    private void saveSettingIfNotExists(String key, String value, Map<String, String> description, String typeHint) {
        if (!systemSettingRepository.findByKey(key).isPresent()) {
            SystemSetting setting = SystemSetting.builder()
                    .key(key)
                    .value(value)
                    .description(description)
                    .typeHint(typeHint)
                    .lastUpdatedAt(LocalDateTime.now()) // Should be set by @LastModifiedDate if auditing is on
                    .build();
            systemSettingRepository.save(setting);
            logger.info("Initialized default setting: {} = {}", key, value);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<SystemSetting> getSetting(String key) {
        return systemSettingRepository.findByKey(key);
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(String key, String defaultValue) {
        return systemSettingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        try {
            return systemSettingRepository.findByKey(key)
                    .map(s -> Integer.parseInt(s.getValue()))
                    .orElse(defaultValue);
        } catch (NumberFormatException e) {
            logger.warn("Setting key '{}' value is not a valid integer. Returning default value '{}'.", key, defaultValue, e);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = systemSettingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAll();
    }

    @Override
    public SystemSetting saveSetting(String key, String value, Map<String, String> description, String typeHint) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElse(SystemSetting.builder().key(key).build());

        setting.setValue(value);
        if (description != null) {
            setting.setDescription(description);
        }
        if (typeHint != null) {
            setting.setTypeHint(typeHint);
        }
        // @LastModifiedDate should handle this if auditing is enabled
        setting.setLastUpdatedAt(LocalDateTime.now());

        logger.info("Saving system setting: {} = {}", key, value);
        return systemSettingRepository.save(setting);
    }

    @Override
    public List<SystemSetting> updateSettings(List<SystemSettingDTO> settingsToUpdate) {
        List<SystemSetting> updatedSettings = settingsToUpdate.stream()
                .map(dto -> {
                    // For description, assume DTO might not pass it if unchanged.
                    // If it's always passed, then no need to fetch existing description.
                    Map<String, String> currentDescription = systemSettingRepository.findByKey(dto.getKey())
                            .map(SystemSetting::getDescription)
                            .orElse(null);
                    if (dto.getDescription() == null) {
                        dto.setDescription(currentDescription);
                    }
                    return saveSetting(dto.getKey(), dto.getValue(), dto.getDescription(), null); // Type hint not in DTO for now
                })
                .collect(Collectors.toList());
        logger.info("Updated {} system settings.", updatedSettings.size());
        return updatedSettings;
    }
}