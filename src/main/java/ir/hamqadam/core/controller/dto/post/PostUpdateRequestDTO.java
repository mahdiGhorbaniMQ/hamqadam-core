package ir.hamqadam.core.controller.dto.post;

import ir.hamqadam.core.model.Post; // For enums and MediaAttachment
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PostUpdateRequestDTO {
    @Size(min = 1, message = "Title cannot be empty if provided")
    private Map<String, String> title; // i18n

    private Map<String, String> contentBody; // i18n
    private Post.ContentBodyType contentBodyType;
    // postType and authorInfo are generally not updatable after creation

    private Post.PostVisibility visibility;

    @Size(max = 20, message = "Too many tags, max 20")
    private List<String> tags;

    @Size(max = 5, message = "Too many categories, max 5")
    private List<String> categoryIds;

    private String featuredImageUrl; // Allow sending "" or null to remove

    @Valid
    private List<Post.MediaAttachment> mediaAttachments;

    // linkedEntityInfo is generally not updatable easily, might require specific logic
    // private PostLinkedEntityInfoDTO linkedEntityInfo;

    private Boolean allowComments;

    private String language;
}