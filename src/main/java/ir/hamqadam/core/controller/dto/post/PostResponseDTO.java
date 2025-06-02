package ir.hamqadam.core.controller.dto.post;

import ir.hamqadam.core.controller.dto.user.UserSummaryDTO; // A new DTO for author summary
import ir.hamqadam.core.controller.dto.team.TeamSummaryDTO;   // A new DTO for author summary
import ir.hamqadam.core.model.Post; // For enums and inner classes
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDTO {
    private String postId;
    private String postType;
    private Map<String, String> title; // i18n
    private Map<String, String> contentBody; // i18n (or an excerpt for list views)
    private Post.ContentBodyType contentBodyType;
    private Map<String, String> excerpt; // i18n

    // Author Information - using summary DTOs
    private Post.AuthorType authorType;
    private String authorId;
    private UserSummaryDTO authorUser; // Populated if authorType is USER
    private TeamSummaryDTO authorTeam; // Populated if authorType is TEAM
    private UserSummaryDTO actingUser; // Summary of user who actually posted/edited

    private Integer version;
    private Post.PostStatus status;
    private Post.PostVisibility visibility;
    private String featuredImageUrl;
    private List<Post.MediaAttachment> mediaAttachments;
    private List<String> tags;
    private List<String> categoryIds; // Or List<CategorySummaryDTO>
    private String language;
    private boolean allowComments;
    private long commentCount;
    private long viewCount;
    private long likeCount;
    private Map<String, Long> reactionCounts;

    private Post.LinkedEntityInfo linkedEntityInfo; // Using model's inner class for response
    private String parentPostId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime scheduledForPublicationAt;
}