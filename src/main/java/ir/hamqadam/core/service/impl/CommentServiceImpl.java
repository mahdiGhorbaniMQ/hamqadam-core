package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.exception.UnauthorizedException;
import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.model.Comment;
import ir.hamqadam.core.model.Post; // Example target entity
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.repository.CommentRepository;
import ir.hamqadam.core.repository.PostRepository; // Example target entity repository
import ir.hamqadam.core.repository.UserRepository;
// import ir.hamqadam.core.service.NotificationService;
import ir.hamqadam.core.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final UserRepository userRepository; // To validate author
    private final PostRepository postRepository; // Example: To validate Post as a target and check if it allows comments
    // Inject other target entity repositories if comments can be on other types
    // private final NotificationService notificationService;

    // Define a default status for new comments, could be configurable
    private static final Comment.CommentStatus DEFAULT_NEW_COMMENT_STATUS = Comment.CommentStatus.APPROVED; // Or PENDING_APPROVAL

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository,
                              UserRepository userRepository,
                              PostRepository postRepository
            /*, NotificationService notificationService */) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository; // Example
        // this.notificationService = notificationService;
    }

    @Override
    public Comment addComment(String targetEntityType,
                              String targetEntityId,
                              String content,
                              String parentCommentId,
                              User actingUser) {

        if (!StringUtils.hasText(content)) {
            throw new ValidationException("Comment content cannot be empty.");
        }
        if (actingUser == null) { // Should be caught by Spring Security ideally
            throw new UnauthorizedException("User must be authenticated to comment.");
        }

        // Validate target entity existence and commentability
        // This section needs to be adapted based on all possible targetEntityTypes
        if ("Post".equalsIgnoreCase(targetEntityType)) { // Example for Post
            Post targetPost = postRepository.findById(targetEntityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post (target entity)", "ID", targetEntityId));
            if (!targetPost.isAllowComments()) {
                throw new ValidationException("Comments are not allowed on this post.");
            }
        } else {
            // Add validation for other target entity types (ProjectUpdate, TaskItem etc.)
            // For now, throw if type is not recognized or handled
            throw new ValidationException("Unsupported target entity type for comments: " + targetEntityType);
        }

        String threadId = null;
        int depthLevel = 0;

        if (StringUtils.hasText(parentCommentId)) {
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Comment", "ID", parentCommentId));
            if (!parent.getTargetEntityId().equals(targetEntityId) || !parent.getTargetEntityType().equals(targetEntityType)) {
                throw new ValidationException("Parent comment does not belong to the same target entity.");
            }
            threadId = StringUtils.hasText(parent.getThreadId()) ? parent.getThreadId() : parent.getCommentId();
            depthLevel = parent.getDepthLevel() + 1;
        }


        Comment newComment = Comment.builder()
                .targetEntityType(targetEntityType)
                .targetEntityId(targetEntityId)
                .authorUserId(actingUser.getUserId())
                .content(content)
                .status(DEFAULT_NEW_COMMENT_STATUS) // Or PENDING_APPROVAL based on system rules
                .parentCommentId(parentCommentId)
                .threadId(threadId) // If null, it's a root comment, threadId could be its own ID post-save
                .depthLevel(depthLevel)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .edited(false)
                .likeCount(0L)
                .build();

        Comment savedComment = commentRepository.save(newComment);

        // If it's a root comment and threadId was null, set its own ID as threadId
        if (savedComment.getThreadId() == null) {
            savedComment.setThreadId(savedComment.getCommentId());
            savedComment = commentRepository.save(savedComment);
        }

        // Increment comment count on the target entity (e.g., Post)
        if ("Post".equalsIgnoreCase(targetEntityType) && savedComment.getStatus() == Comment.CommentStatus.APPROVED) {
            postRepository.findById(targetEntityId).ifPresent(post -> {
                post.setCommentCount(commentRepository.countByTargetEntityTypeAndTargetEntityIdAndStatus(
                        "Post", targetEntityId, Comment.CommentStatus.APPROVED));
                postRepository.save(post);
            });
        }

        logger.info("User '{}' added comment '{}' to entity type '{}', ID '{}'",
                actingUser.getUserId(), savedComment.getCommentId(), targetEntityType, targetEntityId);
        // notificationService.notifyForNewComment(savedComment);
        return savedComment;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> findCommentById(String commentId) {
        return commentRepository.findById(commentId);
    }

    @Override
    @PreAuthorize("@commentSecurityService.canUpdateComment(#commentId, principal.username)")
    public Comment updateComment(String commentId, String newContent, String actingUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "ID", commentId));

        // Permission check: Is actingUser the author or a moderator?
        // This can be handled by @PreAuthorize with a custom security service method
        // if (!comment.getAuthorUserId().equals(actingUserId) /* && !isUserModerator(actingUserId, comment.getTargetEntityType(), comment.getTargetEntityId()) */) {
        //    throw new UnauthorizedException("User not authorized to update this comment.");
        // }

        if (!StringUtils.hasText(newContent)) {
            throw new ValidationException("Comment content cannot be empty.");
        }

        comment.setContent(newContent);
        comment.setEdited(true);
        comment.setUpdatedAt(LocalDateTime.now());
        logger.info("Comment '{}' updated by user '{}'", commentId, actingUserId);
        return commentRepository.save(comment);
    }

    @Override
    @PreAuthorize("@commentSecurityService.canDeleteComment(#commentId, principal.username)")
    public void deleteComment(String commentId, String actingUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "ID", commentId));

        // Permission check: Author or moderator/admin
        // if (!comment.getAuthorUserId().equals(actingUserId) /* && !isUserModerator(...) */ ) {
        //    throw new UnauthorizedException("User not authorized to delete this comment.");
        // }

        // Soft delete by changing status
        comment.setStatus(Comment.CommentStatus.DELETED_BY_AUTHOR); // Or DELETED_BY_MODERATOR
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // Decrement comment count on the target entity if it was an approved comment
        if ("Post".equalsIgnoreCase(comment.getTargetEntityType())) {
            postRepository.findById(comment.getTargetEntityId()).ifPresent(post -> {
                post.setCommentCount(commentRepository.countByTargetEntityTypeAndTargetEntityIdAndStatus(
                        "Post", comment.getTargetEntityId(), Comment.CommentStatus.APPROVED));
                postRepository.save(post);
            });
        }
        logger.info("Comment '{}' (soft) deleted by user '{}'", commentId, actingUserId);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MODERATOR') or hasRole('ROLE_SYSTEM_ADMIN') or @commentSecurityService.isTargetEntityAdmin(#commentId, principal.username)") // Example roles
    public Comment changeCommentStatus(String commentId, Comment.CommentStatus newStatus, String actingUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "ID", commentId));

        Comment.CommentStatus oldStatus = comment.getStatus();
        comment.setStatus(newStatus);
        comment.setUpdatedAt(LocalDateTime.now());
        Comment updatedComment = commentRepository.save(comment);
        logger.info("Comment '{}' status changed from {} to {} by moderator '{}'", commentId, oldStatus, newStatus, actingUserId);

        // Update comment count on parent entity if status changed to/from APPROVED
        if ("Post".equalsIgnoreCase(comment.getTargetEntityType())) {
            if ( (oldStatus != Comment.CommentStatus.APPROVED && newStatus == Comment.CommentStatus.APPROVED) ||
                    (oldStatus == Comment.CommentStatus.APPROVED && newStatus != Comment.CommentStatus.APPROVED) ) {
                postRepository.findById(comment.getTargetEntityId()).ifPresent(post -> {
                    post.setCommentCount(commentRepository.countByTargetEntityTypeAndTargetEntityIdAndStatus(
                            "Post", comment.getTargetEntityId(), Comment.CommentStatus.APPROVED));
                    postRepository.save(post);
                });
            }
        }
        return updatedComment;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> findCommentsByTarget(String targetEntityType, String targetEntityId, Pageable pageable) {
        // For general users, only show APPROVED comments.
        // Moderators might need to see others, so a separate method or a role check here.
        // Default sort by creation date ascending, can be overridden by pageable.
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "createdAt"));
        }
        return commentRepository.findByTargetEntityTypeAndTargetEntityIdAndStatus(
                targetEntityType, targetEntityId, Comment.CommentStatus.APPROVED, effectivePageable);
    }

    @Override
    @Transactional(readOnly = true)
    // @PreAuthorize("hasRole('ROLE_MODERATOR') or @commentSecurityService.isTargetEntityAdminByTargetId(#targetEntityType, #targetEntityId, principal.username)")
    public Page<Comment> findCommentsByTargetAndStatus(String targetEntityType, String targetEntityId, Comment.CommentStatus status, Pageable pageable) {
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "createdAt"));
        }
        return commentRepository.findByTargetEntityTypeAndTargetEntityIdAndStatus(
                targetEntityType, targetEntityId, status, effectivePageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> findRepliesToComment(String parentCommentId, Pageable pageable) {
        // Similarly, only show APPROVED replies.
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "createdAt"));
        }
        return commentRepository.findByParentCommentIdAndStatus(parentCommentId, Comment.CommentStatus.APPROVED, effectivePageable);
    }
}