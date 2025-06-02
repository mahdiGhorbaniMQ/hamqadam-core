package ir.hamqadam.core.service;

import ir.hamqadam.core.model.Post;
import ir.hamqadam.core.model.User; // For actingUser context
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PostService {

    /**
     * Creates a new post, initially as a draft or based on initialStatus.
     *
     * @param title             i18n map of the post title.
     * @param contentBody       i18n map of the post content.
     * @param contentBodyType   Type of the content body (e.g., MARKDOWN, HTML).
     * @param postType          Type of the post (e.g., "general_blog", "idea_proposal").
     * @param authorInfo        Information about the author (User or Team).
     * @param visibility        Visibility of the post.
     * @param initialStatus     Initial status (e.g., DRAFT, PENDING_APPROVAL).
     * @param tags              List of tags.
     * @param categoryIds       List of category IDs.
     * @param featuredImageUrl  Optional URL for a featured image.
     * @param mediaAttachments  List of media attachments.
     * @param linkedEntityInfo  Optional info about an entity this post is linked to.
     * @param allowComments     Whether comments are allowed.
     * @param actingUser        The user performing the creation.
     * @return The created Post object.
     * @throws ir.hamqadam.core.exception.ValidationException if data is invalid.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if author entity not found.
     */
    Post createPost(Map<String, String> title,
                    Map<String, String> contentBody,
                    Post.ContentBodyType contentBodyType,
                    String postType,
                    Post.AuthorInfo authorInfo,
                    Post.PostVisibility visibility,
                    Post.PostStatus initialStatus,
                    List<String> tags,
                    List<String> categoryIds,
                    String featuredImageUrl,
                    List<Post.MediaAttachment> mediaAttachments,
                    Post.LinkedEntityInfo linkedEntityInfo,
                    boolean allowComments,
                    User actingUser);

    /**
     * Finds a post by its ID.
     *
     * @param postId The ID of the post.
     * @return An Optional containing the post if found.
     */
    Optional<Post> findPostById(String postId);

    /**
     * Updates an existing post.
     * Only certain fields might be updatable depending on status and permissions.
     *
     * @param postId            The ID of the post to update.
//     * @param updatedFields     A Post object or DTO containing fields to update.
     * (For this example, specific updatable fields are listed).
     * @param actingUserId      ID of the user performing the update.
     * @return The updated Post object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if post not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    Post updatePost(String postId,
                    Map<String, String> title, // i18n
                    Map<String, String> contentBody, // i18n
                    Post.ContentBodyType contentBodyType,
                    Post.PostVisibility visibility,
                    List<String> tags,
                    List<String> categoryIds,
                    String featuredImageUrl,
                    List<Post.MediaAttachment> mediaAttachments,
                    boolean allowComments,
                    String actingUserId);

    /**
     * Changes the status of a post (e.g., DRAFT -> PUBLISHED, PUBLISHED -> ARCHIVED).
     * This handles the publishing logic.
     *
     * @param postId        The ID of the post.
     * @param newStatus     The new status for the post.
     * @param actingUserId  ID of the user performing the action.
     * @param scheduledFor  Optional: if scheduling publication.
     * @return The updated Post object.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if post not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     * @throws ir.hamqadam.core.exception.ValidationException if status transition is invalid.
     */
    Post changePostStatus(String postId, Post.PostStatus newStatus, String actingUserId, LocalDateTime scheduledFor);


    /**
     * Deletes a post (soft delete by changing status or hard delete).
     * For Phase 1, this might mean changing status to ARCHIVED or a specific DELETED status.
     *
     * @param postId       The ID of the post to delete.
     * @param actingUserId ID of the user performing the deletion.
     * @throws ir.hamqadam.core.exception.ResourceNotFoundException if post not found.
     * @throws ir.hamqadam.core.exception.UnauthorizedException if actingUser lacks permission.
     */
    void deletePost(String postId, String actingUserId);

    /**
     * Finds all posts by a specific author (User or Team) with pagination.
     *
     * @param authorType The type of the author.
     * @param authorId   The ID of the author.
     * @param pageable   Pagination information.
     * @return A Page of Posts by the author.
     */
    Page<Post> findPostsByAuthor(Post.AuthorType authorType, String authorId, Pageable pageable);

    /**
     * Finds posts by type (e.g., "general_blog") with pagination.
     * Typically for public, published posts.
     *
     * @param postType The type of post.
     * @param pageable Pagination information.
     * @return A Page of Posts of the specified type.
     */
    Page<Post> findPostsByTypeAndStatus(String postType, Post.PostStatus status, Pageable pageable);

    /**
     * Finds posts by tag with pagination.
     * Typically for public, published posts.
     * @param tag The tag.
     * @param status The post status.
     * @param pageable Pagination information.
     * @return A Page of Posts.
     */
    Page<Post> findPostsByTagAndStatus(String tag, Post.PostStatus status, Pageable pageable);

    /**
     * Finds posts by category with pagination.
     * Typically for public, published posts.
     * @param categoryId The category ID.
     * @param status The post status.
     * @param pageable Pagination information.
     * @return A Page of Posts.
     */
    Page<Post> findPostsByCategoryAndStatus(String categoryId, Post.PostStatus status, Pageable pageable);

    /**
     * Retrieves a list of all published posts, typically for a blog listing or feed.
     *
     * @param pageable Pagination information.
     * @return A Page of published Posts.
     */
    Page<Post> findAllPublishedPosts(Pageable pageable);

    /**
     * Searches public and published posts based on a query (e.g., title, content).
     * @param query The search query.
     * @param pageable Pagination information.
     * @return A Page of matching public and published Posts.
     */
    Page<Post> searchPublicPublishedPosts(String query, Pageable pageable);

    /**
     * Increments the view count for a post.
     * @param postId The ID of the post.
     */
    void incrementViewCount(String postId);
}