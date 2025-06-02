package ir.hamqadam.core.controller.dto.team;

import ir.hamqadam.core.model.Team; // For TeamVisibility enum
import lombok.Data;
import java.util.Map;

@Data
public class TeamUpdateRequestDTO {
    private Map<String, String> teamName; // i18n
    private Map<String, String> description; // i18n
    private String profilePictureUrl;
    private String coverPictureUrl;
    private Team.TeamVisibility visibility;
    private Boolean membershipApprovalRequired;
    // Team handle is usually not updatable or has strict rules
}