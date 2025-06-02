package ir.hamqadam.core.controller.dto.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamSummaryDTO {
    private String teamId;
    private Map<String, String> teamName; // i18n
    private String teamHandle;
    private String profilePictureUrl;
}