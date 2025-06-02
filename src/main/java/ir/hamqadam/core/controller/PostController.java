package ir.hamqadam.core.controller;

import ir.hamqadam.core.controller.dto.common.MessageResponse;
import ir.hamqadam.core.controller.dto.common.PageableResponseDTO;
import ir.hamqadam.core.controller.dto.post.*;
import ir.hamqadam.core.controller.dto.team.TeamSummaryDTO;
import ir.hamqadam.core.controller.dto.user.UserSummaryDTO;
import ir.hamqadam.core.exception.ResourceNotFoundException;
import ir.hamqadam.core.model.Post;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.model.Team; // For fetching author team details
import ir.hamqadam.core.service.PostService;
import ir.hamqadam.core.service.UserService;
import ir.hamqadam.core.service.TeamService; // For fetching author team details

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

import java.util.stream.Collectors;
// import org.modelmapper.ModelMapper;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final TeamService teamService; // To fetch team details for author summary
    // private final ModelMapper modelMapper;

    @Autowired
    public PostController(PostService postService, UserService userService, TeamService teamService /*, ModelMapper modelMapper*/) {
        this.postService = postService;
        this.userService = userService;
        this.teamService = teamService;
        // this.modelMapper = modelMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponseDTO> createPost(
            @Valid @RequestBody PostCreationRequestDTO creationRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        User actingUser = userService.findUserByEmail(currentUserDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserDetails.getUsername()));

        Post.AuthorInfo authorInfoModel = new Post.AuthorInfo(
                creationRequest.getAuthorInfo().getAuthorType(),
                creationRequest.getAuthorInfo().getAuthorId(),
                actingUser.getUserId() // actingUserId is always the authenticated user
        );

        Post.LinkedEntityInfo linkedEntityInfoModel = null;
        if (creationRequest.getLinkedEntityInfo() != null) {
            linkedEntityInfoModel = new Post.LinkedEntityInfo(
                    creationRequest.getLinkedEntityInfo().getEntityType(),
                    creationRequest.getLinkedEntityInfo().getEntityId()
            );
        }

        Post newPost = postService.createPost(
                creationRequest.getTitle(),
                creationRequest.getContentBody(),
                creationRequest.getContentBodyType(),
                creationRequest.getPostType(),
                authorInfoModel,
                creationRequest.getVisibility(),
                creationRequest.getInitialStatus(),
                creationRequest.getTags(),
                creationRequest.getCategoryIds(),
                creationRequest.getFeaturedImageUrl(),
                creationRequest.getMediaAttachments(), // Assumes DTO uses model's MediaAttachment
                linkedEntityInfoModel,
                creationRequest.isAllowComments(),
                actingUser
        );
        newPost.setLanguage(creationRequest.getLanguage()); // Set language if provided

        return new ResponseEntity<>(convertToPostResponseDTO(newPost), HttpStatus.CREATED);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDTO> getPostById(@PathVariable String postId, @AuthenticationPrincipal UserDetails currentUserDetails) {
        Post post = postService.findPostById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "ID", postId));

        // Increment view count - consider if this should be restricted or more robust
        if (currentUserDetails == null || // Unauthenticated user
                (post.getAuthorInfo() != null && !currentUserDetails.getUsername().equals(
                        userService.findUserById(post.getAuthorInfo().getActingUserId()).map(User::getEmail).orElse(""))
                ) // Not the author
        ) {
            postService.incrementViewCount(postId);
            // Re-fetch post if view count needs to be immediately reflected in this response
            // post = postService.findPostById(postId).orElseThrow(...);
        }

        // Add visibility checks here based on currentUserDetails and post.getVisibility()
        // For simplicity, now returning the post. Service layer or security expressions should enforce this.
        return ResponseEntity.ok(convertToPostResponseDTO(post));
    }

    @PutMapping("/{postId}")
    @PreAuthorize("@postSecurityService.canUpdatePost(#postId, principal.username)")
    public ResponseEntity<PostResponseDTO> updatePost(
            @PathVariable String postId,
            @Valid @RequestBody PostUpdateRequestDTO updateRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Post updatedPost = postService.updatePost(
                postId,
                updateRequest.getTitle(),
                updateRequest.getContentBody(),
                updateRequest.getContentBodyType(),
                updateRequest.getVisibility(),
                updateRequest.getTags(),
                updateRequest.getCategoryIds(),
                updateRequest.getFeaturedImageUrl(),
                updateRequest.getMediaAttachments(),
                updateRequest.getAllowComments() != null ? updateRequest.getAllowComments() : true,
                currentUserDetails.getUsername() // Or User ID
        );
        if (updateRequest.getLanguage() != null) { // Language update example
            updatedPost.setLanguage(updateRequest.getLanguage());
            // updatedPost = postRepository.save(updatedPost); // If service doesn't save this
        }
        return ResponseEntity.ok(convertToPostResponseDTO(updatedPost));
    }

    @PutMapping("/{postId}/status")
    @PreAuthorize("@postSecurityService.canChangePostStatus(#postId, principal.username)")
    public ResponseEntity<PostResponseDTO> changePostStatus(
            @PathVariable String postId,
            @Valid @RequestBody PostStatusUpdateRequestDTO statusRequest,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        Post updatedPost = postService.changePostStatus(
                postId,
                statusRequest.getNewStatus(),
                currentUserDetails.getUsername(), // Or User ID
                statusRequest.getScheduledForPublicationAt()
        );
        return ResponseEntity.ok(convertToPostResponseDTO(updatedPost));
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("@postSecurityService.canDeletePost(#postId, principal.username)")
    public ResponseEntity<MessageResponse> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails currentUserDetails) {
        postService.deletePost(postId, currentUserDetails.getUsername()); // Or User ID
        return ResponseEntity.ok(new MessageResponse("Post (soft) deleted successfully."));
    }

    @GetMapping
    public ResponseEntity<PageableResponseDTO<PostResponseDTO>> listPosts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String categoryId,
            // Add other filters like authorId, visibility etc. as needed
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Post> postPage;
        // This is a simplified filtering logic. A more robust way is to use Criteria API or Querydsl
        // or have more specific service methods.
        if (type != null) {
            postPage = postService.findPostsByTypeAndStatus(type, Post.PostStatus.PUBLISHED, pageable);
        } else if (tag != null) {
            postPage = postService.findPostsByTagAndStatus(tag, Post.PostStatus.PUBLISHED, pageable);
        } else if (categoryId != null) {
            postPage = postService.findPostsByCategoryAndStatus(categoryId, Post.PostStatus.PUBLISHED, pageable);
        } else {
            postPage = postService.findAllPublishedPosts(pageable);
        }

        Page<PostResponseDTO> dtoPage = postPage.map(this::convertToPostResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    @GetMapping("/search")
    public ResponseEntity<PageableResponseDTO<PostResponseDTO>> searchPublicPosts(
            @RequestParam String query,
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Post> postPage = postService.searchPublicPublishedPosts(query, pageable);
        Page<PostResponseDTO> dtoPage = postPage.map(this::convertToPostResponseDTO);
        return ResponseEntity.ok(new PageableResponseDTO<>(dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(), dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isLast(), dtoPage.isFirst(), dtoPage.getNumberOfElements(), dtoPage.isEmpty()));
    }

    // --- Helper method for DTO conversion (Placeholder) ---
    private PostResponseDTO convertToPostResponseDTO(Post post) {
        if (post == null) return null;
        // Use ModelMapper or MapStruct for complex mappings

        UserSummaryDTO authorUserSummary = null;
        TeamSummaryDTO authorTeamSummary = null;
        UserSummaryDTO actingUserSummary = null;

        if (post.getAuthorInfo() != null) {
            if (post.getAuthorInfo().getAuthorType() == Post.AuthorType.USER) {
                userService.findUserById(post.getAuthorInfo().getAuthorId()).ifPresent(u ->
                        authorUserSummary.builder()
                                .userId(u.getUserId())
                                .fullName(u.getFullName())
                                .profilePictureUrl(u.getProfilePictures() != null && !u.getProfilePictures().isEmpty() ? u.getProfilePictures().stream().filter(p -> p.isCurrent()).findFirst().map(User.ProfilePicture::getUrl).orElse(null) : null)
                                .build()
                );
            } else if (post.getAuthorInfo().getAuthorType() == Post.AuthorType.TEAM) {
                teamService.findTeamById(post.getAuthorInfo().getAuthorId()).ifPresent(t ->
                        authorTeamSummary.builder()
                                .teamId(t.getTeamId())
                                .teamName(t.getTeamName())
                                .teamHandle(t.getTeamHandle())
                                .profilePictureUrl(t.getProfilePictureUrl())
                                .build()
                );
            }
            // Acting User Summary
            userService.findUserById(post.getAuthorInfo().getActingUserId()).ifPresent(u ->
                    actingUserSummary.builder()
                            .userId(u.getUserId())
                            .fullName(u.getFullName())
                            .profilePictureUrl(u.getProfilePictures() != null && !u.getProfilePictures().isEmpty() ? u.getProfilePictures().stream().filter(p -> p.isCurrent()).findFirst().map(User.ProfilePicture::getUrl).orElse(null) : null)
                            .build()
            );
        }


        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .title(post.getTitle())
                .contentBody(post.getContentBody()) // Consider sending excerpt for list views
                .contentBodyType(post.getContentBodyType())
                .excerpt(post.getExcerpt())
                .authorType(post.getAuthorInfo() != null ? post.getAuthorInfo().getAuthorType() : null)
                .authorId(post.getAuthorInfo() != null ? post.getAuthorInfo().getAuthorId() : null)
                .authorUser(authorUserSummary)
                .authorTeam(authorTeamSummary)
                .actingUser(actingUserSummary)
                .version(post.getVersion())
                .status(post.getStatus())
                .visibility(post.getVisibility())
                .featuredImageUrl(post.getFeaturedImageUrl())
                .mediaAttachments(post.getMediaAttachments()) // Assuming model's inner class is fine
                .tags(post.getTags())
                .categoryIds(post.getCategoryIds())
                .language(post.getLanguage())
                .allowComments(post.isAllowComments())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .reactionCounts(post.getReactionCounts())
                .linkedEntityInfo(post.getLinkedEntityInfo()) // Assuming model's inner class is fine
                .parentPostId(post.getParentPostId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .scheduledForPublicationAt(post.getScheduledForPublicationAt())
                .build();
    }
}