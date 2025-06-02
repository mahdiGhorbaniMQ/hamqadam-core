package ir.hamqadam.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "comments")
public class Comment {

    @Id
    private String commentId;

    // --- Target Information ---
    @Field("target_entity_type") // e.g., "Post", "ProjectUpdate", "TaskItem"
    private String targetEntityType;

    @Field("target_entity_id") // ID of the Post, ProjectUpdate, etc.
    private String targetEntityId;

    // --- Author & Content ---
    @Field("author_user_id")
    private String authorUserId;

    @Field("content") // User-entered content, stored as is (not typically i18n itself)
    private String content;

    @Field("status")
    private CommentStatus status; // Enum: PENDING_APPROVAL, APPROVED, REJECTED, HIDDEN, DELETED

    // --- Hierarchy (Threading) ---
    @Field("parent_comment_id")
    private String parentCommentId; // Null if root comment

    @Field("thread_id") // ID of the root comment in the thread, for easy fetching
    private String threadId;

    @Field("depth_level") // 0 for root, 1 for first reply, etc.
    private int depthLevel;

    // --- Metadata & Interaction ---
    @Field("like_count")
    private long likeCount;

    @Field("reaction_counts")
    private Map<String, Long> reactionCounts; // e.g., {"👍": 10}

    @Field("mentioned_user_ids")
    private List<String> mentionedUserIds;

    @Field("attachments") // Optional: simple attachments to comments
    private List<CommentAttachment> attachments;

    // --- Timestamps ---
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("is_edited")
    private boolean edited;

    // --- Inner classes ---
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommentAttachment {
        @Field("file_name") private String fileName;
        @Field("file_url") private String fileUrl;
        @Field("file_type") private String fileType; // e.g., "image/png", "application/pdf"
        private Long size; // in bytes
    }

    // --- Enums ---
    public enum CommentStatus {
        PENDING_APPROVAL, APPROVED, REJECTED, HIDDEN_BY_MODERATOR, DELETED_BY_AUTHOR
    }
}