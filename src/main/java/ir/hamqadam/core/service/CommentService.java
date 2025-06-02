package ir.hamqadam.core.service;

import ir.hamqadam.core.model.Comment;
import ir.hamqadam.core.model.User; // For actingUser context
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface CommentService {

    /**
     * Adds a new comment to a target entity.
     *
     * @param targetEntityType  The type of the entity being commented on (e.g., "Post", "ProjectUpdate").
     * @param targetEntityId    The ID of the entity being commented on.
     * @param content           The text content of the comment.
     * @param parentCommentId   Optional ID of the parent comment if this is a reply.
     * @param actingUser        The user creating the comment.
     * @return The created Comment object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if actingUser or targetEntity (or parentComment) not found.
     * @throws ir.hamqadam.core.exception.ValidationException if target entity does not allow comments or content is invalid.
     */
    Comment addComment(String targetEntityType,
                       String targetEntityId,
                       String content,
                       String parentCommentId, // Can be null for a root comment
                       User actingUser);

    /**
     * Finds a comment by its ID.
     *
     * @param commentId The ID of the comment.
     * @return An Optional containing the comment if found.
     */
    Optional<Comment> findCommentById(String commentId);

    /**
     * Updates an existing comment's content.
     * Only the author or a moderator should be able to do this.
     *
     * @param commentId    The ID of the comment to update.
     * @param newContent   The new text content for the comment.
     * @param actingUserId ID of the user performing the update.
     * @return The updated Comment object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if comment not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Comment updateComment(String commentId, String newContent, String actingUserId);

    /**
     * Deletes a comment (soft delete by changing status or hard delete).
     *
     * @param commentId    The ID of the comment to delete.
     * @param actingUserId ID of the user performing the deletion (author or moderator).
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if comment not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    void deleteComment(String commentId, String actingUserId);

    /**
     * Changes the status of a comment (e.g., PENDING_APPROVAL -> APPROVED, APPROVED -> HIDDEN_BY_MODERATOR).
     * Typically performed by a moderator or system admin.
     *
     * @param commentId    The ID of the comment.
     * @param newStatus    The new status for the comment.
     * @param actingUserId ID of the user (moderator/admin) performing the action.
     * @return The updated Comment object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if comment not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks moderation permission.
     */
    Comment changeCommentStatus(String commentId, Comment.CommentStatus newStatus, String actingUserId);

    /**
     * Retrieves a page of comments for a specific target entity.
     * Only approved comments are typically returned for general users.
     * Moderators might see pending/hidden comments.
     *
     * @param targetEntityType The type of the entity.
     * @param targetEntityId   The ID of the entity.
     * @param pageable         Pagination information (should include sorting, e.g., by createdAt).
     * @return A Page of Comments.
     */
    Page<Comment> findCommentsByTarget(String targetEntityType, String targetEntityId, Pageable pageable);

    /**
     * Retrieves a page of comments for a specific target entity filtered by status.
     * Useful for moderation.
     *
     * @param targetEntityType The type of the entity.
     * @param targetEntityId   The ID of the entity.
     * @param status           The comment status to filter by.
     * @param pageable         Pagination information.
     * @return A Page of Comments.
     */
    Page<Comment> findCommentsByTargetAndStatus(String targetEntityType, String targetEntityId, Comment.CommentStatus status, Pageable pageable);


    /**
     * Retrieves replies to a specific parent comment.
     * Only approved replies are typically returned.
     *
     * @param parentCommentId The ID of the parent comment.
     * @param pageable        Pagination information.
     * @return A Page of reply Comments.
     */
    Page<Comment> findRepliesToComment(String parentCommentId, Pageable pageable);
}