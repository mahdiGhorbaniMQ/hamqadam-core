package ir.hamqadam.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.hamqadam.core.controller.dto.post.PostCreationRequestDTO;
import ir.hamqadam.core.controller.dto.post.PostAuthorInfoDTO;
import ir.hamqadam.core.controller.dto.post.PostResponseDTO;
import ir.hamqadam.core.controller.dto.user.UserSummaryDTO; // For author in PostResponseDTO
import ir.hamqadam.core.model.Post;
import ir.hamqadam.core.model.User;
import ir.hamqadam.core.service.PostService;
import ir.hamqadam.core.service.UserService;
import ir.hamqadam.core.service.TeamService; // If team can be an author

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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;

/**
 * API/Controller tests for {@link PostController}.
 * Uses MockMvc to simulate HTTP requests and verify responses for post operations.
 * Services are mocked to isolate controller logic and interactions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class PostControllerIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(PostControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private UserService userService;

    @MockBean
    private TeamService teamService; // Mocked even if not directly used by all PostController methods, for context consistency

    private PostCreationRequestDTO postCreationRequest;
    private User mockAuthenticatedUser;
    private Post mockCreatedPost;
    private Map<String, String> postTitleI18n;
    private Map<String, String> postContentI18n;

    private final String MOCK_USER_EMAIL = "post.creator@example.com";
    private final String MOCK_USER_ID = "user-post-creator-123";

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for PostControllerIntegrationTest");

        postTitleI18n = new HashMap<>();
        postTitleI18n.put("en", "My Test Post Title");
        postTitleI18n.put("fa", "عنوان پست تستی من");

        postContentI18n = new HashMap<>();
        postContentI18n.put("en", "This is the content of my test post.");
        postContentI18n.put("fa", "این محتوای پست تستی من است.");

        PostAuthorInfoDTO authorInfoDTO = new PostAuthorInfoDTO(Post.AuthorType.USER, MOCK_USER_ID);

        postCreationRequest = new PostCreationRequestDTO();
        postCreationRequest.setTitle(postTitleI18n);
        postCreationRequest.setContentBody(postContentI18n);
        postCreationRequest.setContentBodyType(Post.ContentBodyType.MARKDOWN);
        postCreationRequest.setPostType("general_blog");
        postCreationRequest.setAuthorInfo(authorInfoDTO);
        postCreationRequest.setVisibility(Post.PostVisibility.PUBLIC);
        postCreationRequest.setAllowComments(true);
        postCreationRequest.setTags(Collections.singletonList("test"));

        mockAuthenticatedUser = User.builder()
                .userId(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .fullName(Map.of("en", "Post Creator"))
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        Post.AuthorInfo authorInfoModel = new Post.AuthorInfo(
                Post.AuthorType.USER,
                MOCK_USER_ID,
                MOCK_USER_ID // Acting user is the same as author for this test
        );

        mockCreatedPost = Post.builder()
                .postId("post-generated-789")
                .title(postTitleI18n)
                .contentBody(postContentI18n)
                .contentBodyType(Post.ContentBodyType.MARKDOWN)
                .postType("general_blog")
                .authorInfo(authorInfoModel)
                .status(Post.PostStatus.DRAFT) // Assuming service creates as draft initially
                .visibility(Post.PostVisibility.PUBLIC)
                .allowComments(true)
                .tags(Collections.singletonList("test"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .commentCount(0)
                .viewCount(0)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/posts - Create Post Successfully")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"})
    void createPost_whenAuthenticatedAndValidRequest_shouldReturnCreatedPost() throws Exception {
        testLogger.info("Test: createPost_whenAuthenticatedAndValidRequest_shouldReturnCreatedPost");

        // Arrange
        when(userService.findUserByEmail(MOCK_USER_EMAIL)).thenReturn(Optional.of(mockAuthenticatedUser));
        when(postService.createPost(
                anyMap(), anyMap(), any(Post.ContentBodyType.class), anyString(),
                any(Post.AuthorInfo.class), any(Post.PostVisibility.class), any(), // initialStatus can be null
                any(), any(), any(), any(), any(), anyBoolean(), any(User.class)
        )).thenReturn(mockCreatedPost);

        // Also mock user lookup for DTO conversion of author details
        UserSummaryDTO authorSummary = UserSummaryDTO.builder().userId(MOCK_USER_ID).fullName(mockAuthenticatedUser.getFullName()).build();
        when(userService.findUserById(MOCK_USER_ID)).thenReturn(Optional.of(mockAuthenticatedUser));


        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.postId", is(mockCreatedPost.getPostId())))
                .andExpect(jsonPath("$.title.en", is(postTitleI18n.get("en"))))
                .andExpect(jsonPath("$.authorInfo.authorId", is(MOCK_USER_ID)))
                .andExpect(jsonPath("$.authorInfo.authorType", is(Post.AuthorType.USER.toString())));

        testLogger.info("Post creation API call successful for post ID: {}", mockCreatedPost.getPostId());
    }

    @Test
    @DisplayName("POST /api/v1/posts - Unauthenticated Should Return Unauthorized")
    void createPost_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        testLogger.info("Test: createPost_whenUnauthenticated_shouldReturnUnauthorized");

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postCreationRequest)));

        // Assert
        resultActions.andExpect(status().isUnauthorized());
        testLogger.warn("Post creation API call failed with 401 Unauthorized, as expected.");
    }

    @Test
    @DisplayName("POST /api/v1/posts - Invalid Request Body (e.g., blank title) Should Return Bad Request")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"})
    void createPost_whenInvalidRequestBody_shouldReturnBadRequest() throws Exception {
        testLogger.info("Test: createPost_whenInvalidRequestBody_shouldReturnBadRequest");
        // Arrange
        postCreationRequest.setTitle(null); // Invalid: title is required
        when(userService.findUserByEmail(MOCK_USER_EMAIL)).thenReturn(Optional.of(mockAuthenticatedUser));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postCreationRequest)));

        // Assert
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors.title", is("Title cannot be null")));

        testLogger.warn("Post creation API call failed with 400 Bad Request due to invalid input, as expected.");
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - Found")
    @WithMockUser(username = MOCK_USER_EMAIL, roles = {"USER"}) // For view count logic if it checks auth
    void getPostById_whenPostExists_shouldReturnPost() throws Exception {
        testLogger.info("Test: getPostById_whenPostExists_shouldReturnPost");
        // Arrange
        String postId = mockCreatedPost.getPostId();
        when(postService.findPostById(postId)).thenReturn(Optional.of(mockCreatedPost));

        // Mock author details for DTO conversion
        when(userService.findUserById(mockCreatedPost.getAuthorInfo().getAuthorId()))
                .thenReturn(Optional.of(mockAuthenticatedUser)); // Assuming author is the mock user
        when(userService.findUserById(mockCreatedPost.getAuthorInfo().getActingUserId()))
                .thenReturn(Optional.of(mockAuthenticatedUser)); // Assuming acting user is the mock user


        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.postId", is(postId)))
                .andExpect(jsonPath("$.title.en", is(mockCreatedPost.getTitle().get("en"))))
                .andExpect(jsonPath("$.author.userId", is(mockAuthenticatedUser.getUserId())));

        testLogger.info("Get post by ID API call successful for ID: {}", postId);
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - Not Found")
    void getPostById_whenPostNotFound_shouldReturnNotFound() throws Exception {
        testLogger.info("Test: getPostById_whenPostNotFound_shouldReturnNotFound");
        // Arrange
        String nonExistentPostId = "non-existent-post-id";
        when(postService.findPostById(nonExistentPostId)).thenReturn(Optional.empty());

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/posts/{postId}", nonExistentPostId)
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Post not found with ID: " + nonExistentPostId)));
        testLogger.warn("Get post by ID API call failed with 404 Not Found for ID: {}, as expected.", nonExistentPostId);
    }

    // Add more API tests for PostController:
    // - Update post (success, unauthorized, not found, validation errors)
    // - Change post status (publish, archive, schedule - success, unauthorized, invalid transition)
    // - Delete post (success, unauthorized, not found)
    // - List posts (with various filters: type, tag, category, status, pagination, sorting)
    // - Search posts
    // - Test view count increment logic more thoroughly if needed.
    // - Test visibility rules for fetching posts (public vs. private/unlisted requiring specific auth).
}