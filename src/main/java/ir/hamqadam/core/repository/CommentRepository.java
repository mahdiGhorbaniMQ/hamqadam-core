package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    /**
     * Finds all comments for a specific target entity, ordered by creation date.
     *
     * @param targetEntityType The type of the entity (e.g., "Post").
     * @param targetEntityId   The ID of the entity.
     * @param pageable         Pagination and sorting information (e.g., sort by createdAt).
     * @return A page of comments for the specified entity.
     */
    Page<Comment> findByTargetEntityTypeAndTargetEntityId(
            String targetEntityType, String targetEntityId, Pageable pageable);

    /**
     * Finds all comments for a specific target entity and status, ordered by creation date.
     *
     * @param targetEntityType The type of the entity (e.g., "Post").
     * @param targetEntityId   The ID of the entity.
     * @param status           The status of the comment.
     * @param pageable         Pagination and sorting information (e.g., sort by createdAt).
     * @return A page of comments for the specified entity and status.
     */
    Page<Comment> findByTargetEntityTypeAndTargetEntityIdAndStatus(
            String targetEntityType, String targetEntityId, Comment.CommentStatus status, Pageable pageable);


    /**
     * Finds all replies to a specific parent comment.
     *
     * @param parentCommentId The ID of the parent comment.
     * @param pageable        Pagination and sorting information.
     * @return A page of replies.
     */
    Page<Comment> findByParentCommentIdAndStatus(String parentCommentId, Comment.CommentStatus status, Pageable pageable);

    /**
     * Finds comments by a specific author.
     *
     * @param authorUserId The ID of the author.
     * @param pageable     Pagination information.
     * @return A page of comments made by the specified author.
     */
    Page<Comment> findByAuthorUserIdAndStatus(String authorUserId, Comment.CommentStatus status, Pageable pageable);

    /**
     * Counts comments for a specific target entity and status.
     * @param targetEntityType The type of the entity.
     * @param targetEntityId The ID of the entity.
     * @param status The status of the comment.
     * @return The number of comments.
     */
    long countByTargetEntityTypeAndTargetEntityIdAndStatus(
            String targetEntityType, String targetEntityId, Comment.CommentStatus status);
}