package ir.hamqadam.core.controller.dto.post;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostLinkedEntityInfoDTO {
    @NotBlank(message = "Linked entity type cannot be blank")
    private String entityType; // E.g., "Project", "Team", "Routine"

    @NotBlank(message = "Linked entity ID cannot be blank")
    private String entityId;
}