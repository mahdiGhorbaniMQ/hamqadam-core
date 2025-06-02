package ir.hamqadam.core.controller.dto.comment;

import ir.hamqadam.core.controller.dto.user.UserSummaryDTO; // For author details
import ir.hamqadam.core.model.Comment; // For CommentStatus enum
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List; // For potential attachments or nested replies in future
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {
    private String commentId;
    private String targetEntityType;
    private String targetEntityId;
    private UserSummaryDTO author; // Enriched author information
    private String content;
    private Comment.CommentStatus status;
    private String parentCommentId;
    private String threadId;
    private int depthLevel;
    private long likeCount;
    private Map<String, Long> reactionCounts;
    private List<String> mentionedUserIds; // IDs of mentioned users
    // private List<Comment.CommentAttachment> attachments; // If attachments are implemented
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean edited;
    private int replyCount; // Number of direct replies (if needed for UI)
}