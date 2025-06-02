package ir.hamqadam.core.controller.dto.post;

import ir.hamqadam.core.model.Post; // For enums and inner MediaAttachment class
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PostCreationRequestDTO {
    @NotNull(message = "Title cannot be null")
    @Size(min = 1, message = "Title cannot be empty")
    private Map<String, String> title; // i18n

    @NotNull(message = "Content body cannot be null")
    private Map<String, String> contentBody; // i18n

    @NotNull(message = "Content body type cannot be null")
    private Post.ContentBodyType contentBodyType;

    @NotBlank(message = "Post type cannot be blank")
    private String postType; // e.g., "general_blog", "idea_proposal"

    @NotNull(message = "Author information cannot be null")
    @Valid
    private PostAuthorInfoDTO authorInfo;

    private Post.PostVisibility visibility; // Optional, service might default it
    private Post.PostStatus initialStatus; // Optional, service might default to DRAFT

    @Size(max = 20, message = "Too many tags, max 20")
    private List<String> tags;

    @Size(max = 5, message = "Too many categories, max 5")
    private List<String> categoryIds;

    private String featuredImageUrl;

    @Valid // To validate each attachment if it has constraints
    private List<Post.MediaAttachment> mediaAttachments; // Using model's inner class

    @Valid
    private PostLinkedEntityInfoDTO linkedEntityInfo; // Optional

    private boolean allowComments = true; // Default

    private String language; // Default language of the post content
}