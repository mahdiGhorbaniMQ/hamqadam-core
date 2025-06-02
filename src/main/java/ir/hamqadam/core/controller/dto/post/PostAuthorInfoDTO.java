package ir.hamqadam.core.controller.dto.post;

import ir.hamqadam.core.model.Post; // For AuthorType enum
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostAuthorInfoDTO {
    @NotNull(message = "Author type cannot be null")
    private Post.AuthorType authorType; // USER or TEAM

    @NotBlank(message = "Author ID cannot be blank")
    private String authorId; // UserId or TeamId
}