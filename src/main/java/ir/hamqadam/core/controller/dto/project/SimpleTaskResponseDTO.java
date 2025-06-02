package ir.hamqadam.core.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTaskResponseDTO {
    private String taskId; // Could be an index or a generated ID if tasks are sub-documents
    private Map<String, String> taskTitle; // i18n
    private Map<String, String> taskDescription; // i18n
    private String assigneeUserId;
    // private UserSummaryDTO assignee; // Denormalized assignee info
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}