package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query; // For complex queries like text search
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
// import java.util.List; // Already imported if needed previously

@Repository
public interface PostRepository extends MongoRepository<Post, String> {

    /**
     * Finds posts by their type and status.
     *
     * @param postType The type of the post.
     * @param status   The post status.
     * @param pageable Pagination information.
     * @return A page of posts of the specified type and status.
     */
    Page<Post> findByPostTypeAndStatus(String postType, Post.PostStatus status, Pageable pageable);

    /**
     * Finds posts by their status and visibility.
     *
     * @param status   The post status.
     * @param visibility The post visibility.
     * @param pageable Pagination information.
     * @return A page of posts with the specified status and visibility.
     */
    Page<Post> findByStatusAndVisibility(Post.PostStatus status, Post.PostVisibility visibility, Pageable pageable);

    /**
     * Finds posts by author (user or team) and optionally status.
     * Searches within the embedded AuthorInfo object.
     *
     * @param authorType The type of author (USER or TEAM).
     * @param authorId   The ID of the author.
     * @param pageable   Pagination information.
     * @return A page of posts by the specified author.
     */
    Page<Post> findByAuthorInfo_AuthorTypeAndAuthorInfo_AuthorId(
            Post.AuthorType authorType, String authorId, Pageable pageable);

    /**
     * Finds posts by author (user or team) and status.
     * @param authorType The type of author.
     * @param authorId The ID of the author.
     * @param status The post status.
     * @param pageable Pagination information.
     * @return A page of posts.
     */
    Page<Post> findByAuthorInfo_AuthorTypeAndAuthorInfo_AuthorIdAndStatus(
            Post.AuthorType authorType, String authorId, Post.PostStatus status, Pageable pageable);


    /**
     * Finds posts containing a specific tag (case-insensitive) and matching a status.
     *
     * @param tag      The tag to search for.
     * @param status   The post status.
     * @param pageable Pagination information.
     * @return A page of posts containing the specified tag and status.
     */
    Page<Post> findByTagsContainingIgnoreCaseAndStatus(String tag, Post.PostStatus status, Pageable pageable);

    /**
     * Finds posts belonging to a specific category and matching a status.
     *
     * @param categoryId The category ID to search for.
     * @param status     The post status.
     * @param pageable   Pagination information.
     * @return A page of posts in the specified category and status.
     */
    Page<Post> findByCategoryIdsContainingAndStatus(String categoryId, Post.PostStatus status, Pageable pageable);

    /**
     * Finds posts scheduled for publication before a certain date and with a specific status.
     *
     * @param status      The status of the post (e.g., SCHEDULED).
     * @param scheduledAt The date to compare against.
     * @param pageable    Pagination information.
     * @return A page of posts scheduled for publication.
     */
    Page<Post> findByStatusAndScheduledForPublicationAtBefore(
            Post.PostStatus status, LocalDateTime scheduledAt, Pageable pageable);

    /**
     * Finds posts linked to a specific entity.
     * @param entityType The type of the linked entity.
     * @param entityId The ID of the linked entity.
     * @param pageable Pagination information.
     * @return A page of posts linked to the specified entity.
     */
    Page<Post> findByLinkedEntityInfo_EntityTypeAndLinkedEntityInfo_EntityId(
            String entityType, String entityId, Pageable pageable);

    /**
     * For full-text search on title and content (especially i18n fields).
     * Requires a text index in MongoDB on relevant fields (e.g., 'title.en', 'contentBody.en').
     * Example: db.posts.createIndex({"title.en": "text", "contentBody.en": "text", "title.fa": "text", "contentBody.fa": "text"}, {name: "post_text_search_index"})
     *
     * @param searchTerm The term to search for.
     * @param status     The status of the posts to search within.
     * @param visibility The visibility of the posts to search within.
     * @param pageable   Pagination information.
     * @return A page of posts matching the text search, status, and visibility.
     */
    @Query("{ '$and': [ { '$text': { '$search': ?0 } }, { 'status': ?1 }, { 'visibility': ?2 } ] }")
    Page<Post> searchByTextAndStatusAndVisibility(String searchTerm, Post.PostStatus status, Post.PostVisibility visibility, Pageable pageable);

    /**
     * Finds posts by title (case-insensitive, specific language) and status and visibility.
     * This is an example if you don't use full-text search and want to query a specific language field.
     * You would need one such method per language field you want to search if not using text index.
     *
     * @param titleQuery The query string for the title (e.g., English title).
     * @param status     The status of the posts.
     * @param visibility The visibility of the posts.
     * @param pageable   Pagination information.
     * @return A page of posts.
     */
    @Query("{ 'title.en': { $regex: ?0, $options: 'i' }, 'status': ?1, 'visibility': ?2 }") // Example for English title
    Page<Post> findByTitleEnContainingIgnoreCaseAndStatusAndVisibility(String titleQuery, Post.PostStatus status, Post.PostVisibility visibility, Pageable pageable);
    // You might create similar methods for other languages or combine them in the service if needed.
}