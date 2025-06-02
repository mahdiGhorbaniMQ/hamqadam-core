package ir.hamqadam.core.controller.dto.post;

import ir.hamqadam.core.model.Post; // For PostStatus enum
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostStatusUpdateRequestDTO {
    @NotNull(message = "New post status cannot be null")
    private Post.PostStatus newStatus;
    private LocalDateTime scheduledForPublicationAt; // Required if newStatus is SCHEDULED
}