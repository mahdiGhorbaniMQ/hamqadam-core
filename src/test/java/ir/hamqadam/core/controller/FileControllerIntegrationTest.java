package ir.hamqadam.core.controller;

import ir.hamqadam.core.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser; // If upload endpoint is secured
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart; // For file uploads
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

/**
 * API/Controller tests for {@link FileController}.
 * Uses MockMvc to simulate HTTP requests for file uploads and downloads.
 * FileStorageService is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username="mahdighorbanimq@gmail.com", roles={"USER"})
@WithUserDetails("mahdighorbanimq@gmail.com")
class FileControllerIntegrationTest {

    private static final Logger testLogger = LoggerFactory.getLogger(FileControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    private MockMultipartFile mockFile;
    private String mockStoredRelativePath;
    private String mockFileUrl;

    @BeforeEach
    void setUp() {
        testLogger.info("Setting up data for FileControllerIntegrationTest");
        mockFile = new MockMultipartFile(
                "file", // akena (parameter name) in @RequestParam("file")
                "test-file.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, Hamqadam!".getBytes(StandardCharsets.UTF_8)
        );

        mockStoredRelativePath = "uploads/test-file-uuid.txt";
        mockFileUrl = "http://localhost/api/v1/files/" + mockStoredRelativePath;
    }

    @Test
    @DisplayName("POST /api/v1/files/upload - Upload File Successfully")
    @WithMockUser(username = "uploader@example.com", roles = {"USER"}) // Assuming upload is an authenticated action
    void uploadFile_whenAuthenticatedAndValidFile_shouldReturnSuccessResponse() throws Exception {
        testLogger.info("Test: uploadFile_whenAuthenticatedAndValidFile_shouldReturnSuccessResponse");

        // Arrange
        when(fileStorageService.storeFile(any(MockMultipartFile.class), anyString())).thenReturn(mockStoredRelativePath);
        when(fileStorageService.generateFileUrl(mockStoredRelativePath)).thenReturn(mockFileUrl);
        // If subDirectory is not used or is null, then second arg to storeFile should be null or eq(null)

        // Act
        ResultActions resultActions = mockMvc.perform(multipart("/api/v1/files/upload")
                .file(mockFile)
                .param("subDirectory", "uploads") // Optional parameter
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // Assert
        resultActions
                .andExpect(status().isCreated()) // Expect HTTP 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("File uploaded successfully!")))
                .andExpect(jsonPath("$.filePath", is(mockStoredRelativePath)))
                .andExpect(jsonPath("$.fileUrl", is(mockFileUrl)))
                .andExpect(jsonPath("$.fileName", is(mockFile.getOriginalFilename())));

        testLogger.info("File upload API call successful for file: {}", mockFile.getOriginalFilename());
    }

    @Test
    @DisplayName("POST /api/v1/files/upload - Upload Empty File Should Return Bad Request")
    @WithMockUser(username = "uploader@example.com", roles = {"USER"})
    void uploadFile_whenFileIsEmpty_shouldReturnBadRequest() throws Exception {
        testLogger.info("Test: uploadFile_whenFileIsEmpty_shouldReturnBadRequest");
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0] // Empty content
        );

        // Act
        ResultActions resultActions = mockMvc.perform(multipart("/api/v1/files/upload")
                .file(emptyFile)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // Assert (The controller logic directly returns bad request for empty file)
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("File cannot be empty.")));
        testLogger.warn("File upload API call failed with 400 Bad Request due to empty file, as expected.");
    }


    @Test
    @DisplayName("GET /api/v1/files/{filePath} - Download File Successfully")
    void downloadFile_whenFileExists_shouldReturnFileContent() throws Exception {
        testLogger.info("Test: downloadFile_whenFileExists_shouldReturnFileContent");
        // Arrange
        String relativePathForDownload = "uploads/image.png"; // Example path
        byte[] fileContentBytes = "dummy image content".getBytes(StandardCharsets.UTF_8);
        Resource mockResource = new ByteArrayResource(fileContentBytes) {
            @Override
            public String getFilename() {
                return "image.png";
            }
        };
        when(fileStorageService.loadFileAsResource(relativePathForDownload)).thenReturn(Optional.of(mockResource));

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/files/{filePath}", relativePathForDownload)
                // If your path contains subdirectories like "uploads/image.png", adjust the mapping or how you pass it.
                // The current FileController uses "/**" so it should capture "uploads/image.png"
                // For `get("/api/v1/files/{filePath}", "uploads/image.png")` it works if filePath is the full relative path.
                // If the controller splits it, you might need to mock getMimeType from ServletContext if that's used.
                // For the example FileController, HttpServletRequest is used to get the full path after "/api/v1/files/"
                .contentType(MediaType.APPLICATION_OCTET_STREAM)); // Client might specify or accept anything

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"image.png\""))
                // .andExpect(contentType(MediaType.IMAGE_PNG_VALUE)) // This depends on mockServletContext.getMimeType
                .andExpect(content().bytes(fileContentBytes));

        testLogger.info("File download API call successful for path: {}", relativePathForDownload);
    }

    @Test
    @DisplayName("GET /api/v1/files/{filePath} - File Not Found Should Return 404")
    void downloadFile_whenFileDoesNotExist_shouldReturnNotFound() throws Exception {
        testLogger.info("Test: downloadFile_whenFileDoesNotExist_shouldReturnNotFound");
        // Arrange
        String nonExistentRelativePath = "non/existent/file.txt";
        when(fileStorageService.loadFileAsResource(nonExistentRelativePath)).thenReturn(Optional.empty());
        // Or throw ResourceNotFoundException from service if that's the contract:
        // when(fileStorageService.loadFileAsResource(nonExistentRelativePath))
        // .thenThrow(new ir.hamqadam.core.exception.ResourceNotFoundException("File", "path", nonExistentRelativePath));


        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/v1/files/{filePath}", nonExistentRelativePath)
                // The current FileController mapping `/**` might require a slight adjustment in how the path is passed or interpreted
                // For this test, assuming `nonExistentRelativePath` is correctly passed to `loadFileAsResource`.
                // If the path is "non/existent/file.txt", the request URL would be /api/v1/files/non/existent/file.txt
                .contentType(MediaType.APPLICATION_OCTET_STREAM));

        // Assert
        // The FileController throws ResourceNotFoundException which GlobalExceptionHandler turns to 404
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("File not found with path : '" + nonExistentRelativePath + "'")));

        testLogger.warn("File download API call failed with 404 Not Found for path: {}, as expected.", nonExistentRelativePath);
    }

    // Add more tests:
    // - Upload file with disallowed type (if FileStorageService implements type checking)
    // - Upload file exceeding size limits
    // - Download file when FileStorageService throws an internal error
    // - Test security of upload/download endpoints if specific rules apply (e.g., only owner can download private files)
}