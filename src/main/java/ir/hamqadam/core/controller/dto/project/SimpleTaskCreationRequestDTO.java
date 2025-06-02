package ir.hamqadam.core.controller.dto.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Map;

@Data
public class SimpleTaskCreationRequestDTO {
    @NotEmpty(message = "Task title cannot be empty")
    private Map<String, String> taskTitle; // i18n
    private Map<String, String> taskDescription; // i18n, optional
    private String assigneeUserId; // Optional
    // Add other fields like priority, dueDate if needed for simple tasks
}