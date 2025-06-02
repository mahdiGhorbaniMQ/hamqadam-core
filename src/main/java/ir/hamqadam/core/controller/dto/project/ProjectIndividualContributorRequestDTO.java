package ir.hamqadam.core.controller.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

@Data
public class ProjectIndividualContributorRequestDTO {
    @NotBlank(message = "User ID for contributor cannot be blank")
    private String userId;
    private String roleInProject;
    private Map<String, String> contributionDescription; // i18n
}