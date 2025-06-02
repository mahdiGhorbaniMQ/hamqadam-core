package ir.hamqadam.core.controller.dto.routine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineTaskOrActionDTO { // For response
    // private String taskId; // If tasks get individual IDs
    private Map<String, String> taskTitle; // i18n
    private Map<String, String> taskDescription; // i18n
    private String assignedToRoleInRoutine;
    private String assignedToUserId;
    // private UserSummaryDTO assigneeUser; // For enriched response
    private String dueCondition;
    private String status; // e.g., "PENDING", "COMPLETED" for a specific occurrence
    // private String trackingPostId; // If a post is generated for tracking
}