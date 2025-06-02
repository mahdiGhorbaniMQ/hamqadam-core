package ir.hamqadam.core.controller.dto.comment;

import ir.hamqadam.core.model.Comment; // For CommentStatus enum
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentStatusUpdateRequestDTO {
    @NotNull(message = "New comment status cannot be null")
    private Comment.CommentStatus newStatus;
}