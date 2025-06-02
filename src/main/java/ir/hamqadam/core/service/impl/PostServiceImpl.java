package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.exception.UnauthorizedException;
import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.Post;
import ir.hamqadam.core.model.Team;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.PostRepository;
import ir.hamqadam.core.repository.UserRepository;
import ir.hamqadam.core.repository.TeamRepository;
// import ir.hamqadam.core.service.NotificationService;
import ir.hamqadam.core.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    // private final NotificationService notificationService;

    @Autowired
    public PostServiceImpl(PostRepository postRepository,
                           UserRepository userRepository,
                           TeamRepository teamRepository
            /*, NotificationService notificationService */) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        // this.notificationService = notificationService;
    }

    @Override
    public Post createPost(Map<String, String> title,
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
                           User actingUser) {

        if (authorInfo == null || !StringUtils.hasText(authorInfo.getAuthorId())) {
            throw new ValidationException("Author information is required.");
        }
        if (authorInfo.getAuthorType() == Post.AuthorType.USER) {
            userRepository.findById(authorInfo.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author (User)", "ID", authorInfo.getAuthorId()));
        } else if (authorInfo.getAuthorType() == Post.AuthorType.TEAM) {
            teamRepository.findById(authorInfo.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author (Team)", "ID", authorInfo.getAuthorId()));
        }
        // Ensure actingUser is correctly set in authorInfo
        authorInfo.setActingUserId(actingUser.getUserId());


        Post newPost = Post.builder()
                .title(title)
                .contentBody(contentBody)
                .contentBodyType(contentBodyType)
                .postType(postType)
                .authorInfo(authorInfo)
                .visibility(visibility != null ? visibility : Post.PostVisibility.PUBLIC) // Default visibility
                .status(initialStatus != null ? initialStatus : Post.PostStatus.DRAFT) // Default status
                .tags(tags)
                .categoryIds(categoryIds)
                .featuredImageUrl(featuredImageUrl)
                .mediaAttachments(mediaAttachments)
                .linkedEntityInfo(linkedEntityInfo)
                .allowComments(allowComments)
                .commentCount(0)
                .viewCount(0)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        logger.info("Creating new post of type '{}' by author type '{}', ID '{}', acting user '{}'",
                postType, authorInfo.getAuthorType(), authorInfo.getAuthorId(), actingUser.getUserId());
        return postRepository.save(newPost);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findPostById(String postId) {
        // Additional logic might be needed here to check visibility based on the requesting user
        // For now, it returns the post if it exists. Controller should handle visibility.
        return postRepository.findById(postId);
    }

    @Override
    @PreAuthorize("@postSecurityService.canUpdatePost(#postId, principal.username)")
    public Post updatePost(String postId,
                           Map<String, String> title,
                           Map<String, String> contentBody,
                           Post.ContentBodyType contentBodyType,
                           Post.PostVisibility visibility,
                           List<String> tags,
                           List<String> categoryIds,
                           String featuredImageUrl,
                           List<Post.MediaAttachment> mediaAttachments,
                           boolean allowComments,
                           String actingUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "postId", postId));

        // checkUpdatePermission(post, actingUserId); // Or use @PreAuthorize

        if (title != null) post.setTitle(title);
        if (contentBody != null) post.setContentBody(contentBody);
        if (contentBodyType != null) post.setContentBodyType(contentBodyType);
        if (visibility != null) post.setVisibility(visibility);
        if (tags != null) post.setTags(tags);
        if (categoryIds != null) post.setCategoryIds(categoryIds);
        if (featuredImageUrl != null) post.setFeaturedImageUrl(featuredImageUrl); // Allow unsetting by passing "" or null
        if (mediaAttachments != null) post.setMediaAttachments(mediaAttachments);
        post.setAllowComments(allowComments);

        post.setUpdatedAt(LocalDateTime.now());
        // If status is PUBLISHED, perhaps create a new version or log change. For Phase 1, direct update.
        if (post.getStatus() == Post.PostStatus.PUBLISHED) {
            post.setVersion(post.getVersion() == null ? 2 : post.getVersion() + 1);
        }

        logger.info("Post '{}' updated by user '{}'", postId, actingUserId);
        return postRepository.save(post);
    }

    private void checkUpdatePermission(Post post, String actingUserId) {
        // User can update if they are the original acting user in authorInfo,
        // or if they are an admin of the authoring team (if author is team),
        // or a system admin.
        boolean canUpdate = false;
        if (post.getAuthorInfo().getActingUserId().equals(actingUserId)) {
            canUpdate = true;
        } else if (post.getAuthorInfo().getAuthorType() == Post.AuthorType.TEAM) {
            Team authorTeam = teamRepository.findById(post.getAuthorInfo().getAuthorId()).orElse(null);
            if (authorTeam != null && authorTeam.getMembers().stream()
                    .anyMatch(m -> m.getUserId().equals(actingUserId) && m.getRoles().contains("ADMIN"))) { // Assuming "ADMIN" role in team
                canUpdate = true;
            }
        }
        // Add check for system admin role here if needed
        if (!canUpdate) {
            throw new UnauthorizedException("User " + actingUserId + " is not authorized to update post " + post.getPostId());
        }
    }


    @Override
    @PreAuthorize("@postSecurityService.canChangePostStatus(#postId, principal.username)")
    public Post changePostStatus(String postId, Post.PostStatus newStatus, String actingUserId, LocalDateTime scheduledFor) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "postId", postId));

        // checkUpdatePermission(post, actingUserId); // Status change might have stricter permissions

        // Validate status transition (e.g., cannot publish a rejected post directly)
        // For Phase 1, keep it simple.
        if (newStatus == Post.PostStatus.PUBLISHED && post.getStatus() != Post.PostStatus.DRAFT && post.getStatus() != Post.PostStatus.PENDING_APPROVAL && post.getStatus() != Post.PostStatus.SCHEDULED) {
            // Allow re-publishing archived for example, or from draft/pending
            // For simplicity, allow if not already published or rejected
            if(post.getStatus() == Post.PostStatus.REJECTED) {
                throw new ValidationException("Cannot publish a rejected post. Change status to draft first.");
            }
        }

        post.setStatus(newStatus);
        if (newStatus == Post.PostStatus.PUBLISHED) {
            post.setPublishedAt(LocalDateTime.now());
            post.setScheduledForPublicationAt(null); // Clear schedule if publishing now
            // notificationService.notifyFollowersOfAuthor(post.getAuthorInfo(), post);
        } else if (newStatus == Post.PostStatus.SCHEDULED) {
            if (scheduledFor == null || scheduledFor.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Scheduled publication date must be in the future.");
            }
            post.setScheduledForPublicationAt(scheduledFor);
            post.setPublishedAt(null);
        }

        post.setUpdatedAt(LocalDateTime.now());
        logger.info("Post '{}' status changed to {} by user '{}'", postId, newStatus, actingUserId);
        return postRepository.save(post);
    }

    @Override
    @PreAuthorize("@postSecurityService.canDeletePost(#postId, principal.username)")
    public void deletePost(String postId, String actingUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "postId", postId));

        // checkUpdatePermission(post, actingUserId); // Or specific delete permission

        // For Phase 1, soft delete by changing status to ARCHIVED or a specific DELETED status
        // If hard delete: postRepository.delete(post);
        post.setStatus(Post.PostStatus.ARCHIVED); // Or a new DELETED status
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
        logger.info("Post '{}' (soft) deleted by user '{}'", postId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findPostsByAuthor(Post.AuthorType authorType, String authorId, Pageable pageable) {
        // Consider adding status filter, e.g., only published or draft+published for author
        return postRepository.findByAuthorInfo_AuthorTypeAndAuthorInfo_AuthorId(authorType, authorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findPostsByTypeAndStatus(String postType, Post.PostStatus status, Pageable pageable) {
        return postRepository.findByPostTypeAndStatus(postType, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findPostsByTagAndStatus(String tag, Post.PostStatus status, Pageable pageable) {
        return postRepository.findByTagsContainingIgnoreCaseAndStatus(tag, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findPostsByCategoryAndStatus(String categoryId, Post.PostStatus status, Pageable pageable) {
        return postRepository.findByCategoryIdsContainingAndStatus(categoryId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findAllPublishedPosts(Pageable pageable) {
        return postRepository.findByStatusAndVisibility(Post.PostStatus.PUBLISHED, Post.PostVisibility.PUBLIC, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> searchPublicPublishedPosts(String query, Pageable pageable) {
        // For Phase 1: Simple search, requires text index on MongoDB for title/content for efficient search.
        // Or specific repository methods for title/contentBody.key CONTAINS query.
        // Example using a custom query if text index "post_text_index" is on title and contentBody maps:
        // return postRepository.searchByTextAndStatusAndVisibility(query, Post.PostStatus.PUBLISHED, Post.PostVisibility.PUBLIC, pageable);
        logger.warn("SearchPublicPublishedPosts basic implementation used. Query: {}", query);
        if (!StringUtils.hasText(query)) {
            return postRepository.findByStatusAndVisibility(Post.PostStatus.PUBLISHED, Post.PostVisibility.PUBLIC, pageable);
        }
        // Placeholder for actual search logic, repository would need a method like:
        // Page<Post> findByTitleContainingIgnoreCaseAndStatusAndVisibility(String titleQuery, PostStatus status, PostVisibility visibility, Pageable pageable);
        return postRepository.findByStatusAndVisibility(Post.PostStatus.PUBLISHED, Post.PostVisibility.PUBLIC, pageable);
    }

    @Override
    public void incrementViewCount(String postId) {
        // This should ideally be an atomic operation if high concurrency is expected.
        // For MongoDB, $inc operator is good.
        // For simplicity here, find and save. Could be optimized.
        postRepository.findById(postId).ifPresent(post -> {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post); // This might not be the most performant way for high traffic
        });
    }
}