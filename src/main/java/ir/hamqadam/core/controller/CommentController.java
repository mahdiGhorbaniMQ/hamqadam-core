package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.comment.*;
import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.common.PageableResponseDTO;
import ir.hamqadam.core.controller.dto.user.UserSummaryDTO;
import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.model.Comment;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.CommentService;
import ir.hamqadam.core.service.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;
// import org.modelmapper.ModelMapper;

@RestController
@RequestMapping("/api/v1") // Base path, specific paths defined in methods
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;
    // private final ModelMapper modelMapper;

    @Autowired
    public CommentController(CommentService commentService, UserService userService /*, ModelMapper modelMapper */) {
        this.commentService = commentService;
        this.userService = userService;
        // this.modelMapper = modelMapper;
    }

    @PostMapping("/{targetEntityType}/{targetEntityId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable String targetEntityType,
            @PathVariable String targetEntityId,
            @Valid @RequestBody CommentCreationRequestDTO creationRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User actingUser = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));

        Comment newComment = commentService.addComment(
                targetEntityType,
                targetEntityId,
                creationRequest.getContent(),
                creationRequest.getParentCommentId(),
                actingUser
        );
        return new ResponseEntity<>(convertToCommentResponseDTO(newComment), HttpStatus.CREATED);
    }

    @GetMapping("/{targetEntityType}/{targetEntityId}/comments")
    public ResponseEntity<PageableResponseDTO<CommentResponseDTO>> getCommentsForTarget(
            @PathVariable String targetEntityType,
            @PathVariable String targetEntityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        // Service method should by default return only APPROVED comments for public view
        Page<Comment> commentPage = commentService.findCommentsByTarget(targetEntityType, targetEntityId, pageable);
        Page<CommentResponseDTO> dtoPage = commentPage.map(this::convertToCommentResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<PageableResponseDTO<CommentResponseDTO>> getCommentReplies(
            @PathVariable String commentId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Comment> replyPage = commentService.findRepliesToComment(commentId, pageable);
        Page<CommentResponseDTO> dtoPage = replyPage.map(this::convertToCommentResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }


    @PutMapping("/comments/{commentId}")
    @PreAuthorize("@commentSecurityService.canUpdateComment(#commentId, principal.username)")
    public ResponseEntity<CommentResponseDTO> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody CommentUpdateRequestDTO updateRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Comment updatedComment = commentService.updateComment(
                commentId,
                updateRequest.getContent(),
                currentUserDetails.getUsername() // Or User ID
        );
        return ResponseEntity.ok(convertToCommentResponseDTO(updatedComment));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("@commentSecurityService.canDeleteComment(#commentId, principal.username)")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        commentService.deleteComment(commentId, currentUserDetails.getUsername()); // Or User ID
        return ResponseEntity.ok(new MessageResponse("Comment (soft) deleted successfully."));
    }

    @PutMapping("/comments/{commentId}/status")
    @PreAuthorize("hasRole('ROLE_MODERATOR') or hasRole('ROLE_SYSTEM_ADMIN') or @commentSecurityService.isTargetEntityAdminForComment(#commentId, principal.username)")
    public ResponseEntity<CommentResponseDTO> changeCommentStatus(
            @PathVariable String commentId,
            @Valid @RequestBody CommentStatusUpdateRequestDTO statusRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Comment updatedComment = commentService.changeCommentStatus(
                commentId,
                statusRequest.getNewStatus(),
                currentUserDetails.getUsername() // Or User ID
        );
        return ResponseEntity.ok(convertToCommentResponseDTO(updatedComment));
    }

    // --- Helper method for DTO conversion (Placeholder) ---
    private CommentResponseDTO convertToCommentResponseDTO(Comment comment) {
        if (comment == null) return null;
        // Use ModelMapper or MapStruct for complex mappings

        UserSummaryDTO authorSummary = null;
        if (comment.getAuthorUserId() != null) {
            // This is an N+1 problem if converting a list of comments.
            // Consider batch fetching authors or a more optimized approach for lists.
            Optional<User> authorOpt = userService.findUserById(comment.getAuthorUserId());
            if (authorOpt.isPresent()) {
                User author = authorOpt.get();
                authorSummary = UserSummaryDTO.builder()
                        .userId(author.getUserId())
                        .fullName(author.getFullName())
                        .profilePictureUrl(author.getProfilePictures() != null && !author.getProfilePictures().isEmpty() ?
                                author.getProfilePictures().stream().filter(User.ProfilePicture::isCurrent).findFirst().map(User.ProfilePicture::getUrl).orElse(null)
                                : null)
                        .build();
            }
        }

        // Calculate reply count (can be expensive, better to denormalize on Comment entity if frequently needed)
        // int replyCount = commentService.countReplies(comment.getCommentId()); // Example

        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .targetEntityType(comment.getTargetEntityType())
                .targetEntityId(comment.getTargetEntityId())
                .author(authorSummary)
                .content(comment.getContent())
                .status(comment.getStatus())
                .parentCommentId(comment.getParentCommentId())
                .threadId(comment.getThreadId())
                .depthLevel(comment.getDepthLevel())
                .likeCount(comment.getLikeCount())
                .reactionCounts(comment.getReactionCounts())
                .mentionedUserIds(comment.getMentionedUserIds())
                // .attachments(comment.getAttachments()) // Assuming model's inner class is fine
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .edited(comment.isEdited())
                // .replyCount(replyCount) // Add if reply count is needed
                .build();
    }
}