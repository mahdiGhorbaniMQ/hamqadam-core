package ir.hamqadam.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ir.hamqadam.core.controller.dto.comment.CommentCreationRequestDTO;
import ir.hamqadam.core.controller.dto.comment.CommentResponseDTO;
import ir.hamqadam.core.controller.dto.comment.CommentUpdateRequestDTO;
import ir.hamqadam.core.controller.dto.user.UserSummaryDTO;
import ir.hamqadam.core.model.Comment;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.CommentService;
import ir.hamqadam.core.service.UserService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;


/**
 * API/Controller tests for {@link CommentController}.
 * Uses MockMvc to simulate HTTP requests and verify responses for comment operations.
 * Services are mocked to isolate controller logic and interactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class CommentControllerIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(CommentControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    private static ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private UserService userService; // For fetching actingUser and author details for DTO

    private CommentCreationRequestDTO commentCreationRequest;
    private CommentUpdateRequestDTO commentUpdateRequest;
    private User mockAuthenticatedUser;
    private Comment mockComment;
    private UserSummaryDTO mockAuthorSummary;

    private final String MOCK_USER_EMAIL = "commenter@example.com";
    private final String MOCK_USER_ID = "user-commenter-789";
    private final String TARGET_ENTITY_TYPE_POST = "Post"; // Consistent with CommentService/Model
    private final String TARGET_ENTITY_ID_POST = "post-xyz-123";
    private final String MOCK_COMMENT_ID = "comment-abc-456";


    @BeforeAll
    static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for CommentControllerIntegrationTest");

        commentCreationRequest = new CommentCreationRequestDTO();
        commentCreationRequest.setContent("This is a test comment.");
        commentCreationRequest.setParentCommentId(null); // Root comment

        commentUpdateRequest = new CommentUpdateRequestDTO();
        commentUpdateRequest.setContent("This is an updated test comment.");

        mockAuthenticatedUser = User.builder()
                .userId(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .fullName(Map.of("en", "Test Commenter"))
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        mockAuthorSummary = UserSummaryDTO.builder()
                .userId(MOCK_USER_ID)
                .fullName(mockAuthenticatedUser.getFullName())
                .profilePictureUrl(null) // Or some mock URL
                .build();

        mockComment = Comment.builder()
                .commentId(MOCK_COMMENT_ID)
                .targetEntityType(TARGET_ENTITY_TYPE_POST)
                .targetEntityId(TARGET_ENTITY_ID_POST)
                .authorUserId(MOCK_USER_ID)
                .content(commentCreationRequest.getContent())
                .status(Comment.CommentStatus.APPROVED)
                .depthLevel(0)
                .threadId(MOCK_COMMENT_ID) // For a root comment, threadId is often its own ID
                .createdAt(LocalDateTime.now().withNano(0))
                .updatedAt(LocalDateTime.now().withNano(0))
                .likeCount(0)
                .build();
    }

    @Test
    @DisplayName("POST /{targetEntityType}/{targetEntityId}/comments - Add Comment Successfully")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"})
    void addComment_whenAuthenticatedAndValidRequest_shouldReturnCreatedComment() throws Exception {
        testLogger.info("Test: addComment_whenAuthenticatedAndValidRequest_shouldReturnCreatedComment");

        // Arrange
        when(userService.findUserByEmail(MOCK_USER_EMAIL)).thenReturn(Optional.of(mockAuthenticatedUser));
        when(commentService.addComment(
                eq(TARGET_ENTITY_TYPE_POST),
                eq(TARGET_ENTITY_ID_POST),
                anyString(), // content
                any(),       // parentCommentId (can be null)
                any(User.class)  // actingUser
        )).thenReturn(mockComment);

        // For DTO conversion
        when(userService.findUserById(MOCK_USER_ID)).thenReturn(Optional.of(mockAuthenticatedUser));


        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/{targetEntityType}/{targetEntityId}/comments",
                TARGET_ENTITY_TYPE_POST, TARGET_ENTITY_ID_POST)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.commentId", is(mockComment.getCommentId())))
                .andExpect(jsonPath("$.content", is(commentCreationRequest.getContent())))
                .andExpect(jsonPath("$.author.userId", is(MOCK_USER_ID)));

        testLogger.info("Add comment API call successful for comment ID: {}", mockComment.getCommentId());
    }

    @Test
    @DisplayName("POST /{targetEntityType}/{targetEntityId}/comments - Blank Content Should Return Bad Request")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"})
    void addComment_whenContentIsBlank_shouldReturnBadRequest() throws Exception {
        testLogger.info("Test: addComment_whenContentIsBlank_shouldReturnBadRequest");
        // Arrange
        commentCreationRequest.setContent(""); // Invalid: blank content
        when(userService.findUserByEmail(MOCK_USER_EMAIL)).thenReturn(Optional.of(mockAuthenticatedUser));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/{targetEntityType}/{targetEntityId}/comments",
                TARGET_ENTITY_TYPE_POST, TARGET_ENTITY_ID_POST)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors.content", is("Comment content cannot be blank"))); // Or "must be between 1 and 5000..."

        testLogger.warn("Add comment API call failed with 400 Bad Request due to blank content, as expected.");
    }


    @Test
    @DisplayName("GET /{targetEntityType}/{targetEntityId}/comments - List Comments Successfully")
    void listCommentsForTarget_shouldReturnPageOfComments() throws Exception {
        testLogger.info("Test: listCommentsForTarget_shouldReturnPageOfComments");
        // Arrange
        Page<Comment> commentPage = new PageImpl<>(List.of(mockComment), PageRequest.of(0, 20), 1);
        when(commentService.findCommentsByTarget(
                eq(TARGET_ENTITY_TYPE_POST),
                eq(TARGET_ENTITY_ID_POST),
                any(Pageable.class)
        )).thenReturn(commentPage);

        // For DTO conversion
        when(userService.findUserById(MOCK_USER_ID)).thenReturn(Optional.of(mockAuthenticatedUser));

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/{targetEntityType}/{targetEntityId}/comments",
                TARGET_ENTITY_TYPE_POST, TARGET_ENTITY_ID_POST)
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].commentId", is(mockComment.getCommentId())))
                .andExpect(jsonPath("$.content[0].author.userId", is(MOCK_USER_ID)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        testLogger.info("List comments API call successful for target: {}/{}", TARGET_ENTITY_TYPE_POST, TARGET_ENTITY_ID_POST);
    }

    @Test
    @DisplayName("PUT /comments/{commentId} - Update Comment Successfully")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"}) // Assuming author can update
    void updateComment_whenAuthorized_shouldReturnUpdatedComment() throws Exception {
        testLogger.info("Test: updateComment_whenAuthorized_shouldReturnUpdatedComment");
        // Arrange
        Comment updatedMockComment = Comment.builder()
                .commentId(MOCK_COMMENT_ID)
                .targetEntityType(TARGET_ENTITY_TYPE_POST)
                .targetEntityId(TARGET_ENTITY_ID_POST)
                .authorUserId(MOCK_USER_ID)
                .content(commentUpdateRequest.getContent()) // Updated content
                .status(Comment.CommentStatus.APPROVED)
                .edited(true)
                .createdAt(mockComment.getCreatedAt())
                .updatedAt(LocalDateTime.now().withNano(0))
                .build();

        when(commentService.updateComment(
                eq(MOCK_COMMENT_ID),
                eq(commentUpdateRequest.getContent()),
                eq(MOCK_USER_EMAIL) // Controller passes username from principal
        )).thenReturn(updatedMockComment);

        // For DTO conversion
        when(userService.findUserById(MOCK_USER_ID)).thenReturn(Optional.of(mockAuthenticatedUser));

        // Act
        ResultActions resultActions = mockMvc.perform(put("/api/v1/comments/{commentId}", MOCK_COMMENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentUpdateRequest)));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.commentId", is(MOCK_COMMENT_ID)))
                .andExpect(jsonPath("$.content", is(commentUpdateRequest.getContent())))
                .andExpect(jsonPath("$.edited", is(true)));

        testLogger.info("Update comment API call successful for comment ID: {}", MOCK_COMMENT_ID);
    }

    @Test
    @DisplayName("PUT /comments/{commentId} - Update Comment Unauthorized")
    @WithMockUser(username = "another.user@example.com", roles = {"USER"}) // Different user
    void updateComment_whenUnauthorized_shouldReturnForbidden() throws Exception {
        testLogger.info("Test: updateComment_whenUnauthorized_shouldReturnForbidden");
        // Arrange
        // Simulate service throwing UnauthorizedException when permission check fails
        when(commentService.updateComment(
                eq(MOCK_COMMENT_ID),
                eq(commentUpdateRequest.getContent()),
                eq("another.user@example.com")
        )).thenThrow(new ir.hamqadam.core.exception.UnauthorizedException("User not authorized to update this comment."));

        // Act
        ResultActions resultActions = mockMvc.perform(put("/api/v1/comments/{commentId}", MOCK_COMMENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentUpdateRequest)));

        // Assert: Expect 403 Forbidden (or whatever your @commentSecurityService + GlobalExceptionHandler produce)
        resultActions.andExpect(status().isForbidden());
        testLogger.warn("Update comment API call failed with 403 Forbidden as expected for unauthorized user.");
    }


    @Test
    @DisplayName("DELETE /comments/{commentId} - Delete Comment Successfully by Author")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"}) // Author deletes
    void deleteComment_whenAuthorized_shouldReturnSuccessMessage() throws Exception {
        testLogger.info("Test: deleteComment_whenAuthorized_shouldReturnSuccessMessage");
        // Arrange
        doNothing().when(commentService).deleteComment(eq(MOCK_COMMENT_ID), eq(MOCK_USER_EMAIL));

        // Act
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/comments/{commentId}", MOCK_COMMENT_ID));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Comment (soft) deleted successfully.")));

        verify(commentService, times(1)).deleteComment(MOCK_COMMENT_ID, MOCK_USER_EMAIL);
        testLogger.info("Delete comment API call successful for comment ID: {}", MOCK_COMMENT_ID);
    }

    // Add more API tests for CommentController:
    // - Updating a comment that doesn't exist (should return 404).
    // - Deleting a comment that doesn't exist.
    // - Changing comment status by a moderator (success, unauthorized).
    // - Listing replies to a comment.
    // - Test edge cases for parentCommentId during creation.
}