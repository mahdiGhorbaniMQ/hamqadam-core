package ir.hamqadam.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map; // For potential i18n description

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "system_settings")
public class SystemSetting {

    @Id
    private String key; // The setting key, e.g., "site_name", "max_team_members"

    @Field("value")
    private String value; // The setting value, stored as String, to be cast as needed

    @Field("description")
    private Map<String, String> description; // i18n: Optional description of the setting

    @Field("type_hint")
    private String typeHint; // Optional: e.g., "STRING", "INTEGER", "BOOLEAN", "JSON_STRING"

    @LastModifiedDate
    @Field("last_updated_at")
    private LocalDateTime lastUpdatedAt;
}