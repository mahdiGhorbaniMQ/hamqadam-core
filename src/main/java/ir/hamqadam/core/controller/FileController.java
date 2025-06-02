package ir.hamqadam.core.controller;

import ir.hamqadam.core.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // For upload endpoint example
import org.springframework.http.HttpStatus; // For upload endpoint example

import java.io.IOException;
import java.util.Map; // For upload endpoint example

@RestController
@RequestMapping("/api/v1/files") // Matches hamqadam.file-storage.base-serve-url
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;

    @Autowired
    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // Example: An endpoint to upload files (you might put this in other relevant controllers)
    // This is just a generic example. Secure it properly with @PreAuthorize.
    @PostMapping("/upload")
    // @PreAuthorize("isAuthenticated()") // Example security
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "subDirectory", required = false) String subDirectory) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File cannot be empty."));
        }
        try {
            String relativePath = fileStorageService.storeFile(file, subDirectory);
            String fileUrl = fileStorageService.generateFileUrl(relativePath);

            // Return the path and URL
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "File uploaded successfully!",
                    "filePath", relativePath,
                    "fileUrl", fileUrl,
                    "fileName", file.getOriginalFilename(),
                    "fileType", file.getContentType(),
                    "fileSize", file.getSize()
            ));
        } catch (Exception e) {
            logger.error("Could not upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage()));
        }
    }


    // Endpoint to download/view files
    // The path variable here needs to capture the full relative path including subdirectories.
    // Using "**" allows capturing paths with slashes.
    @GetMapping("/**") // This will capture everything after /api/v1/files/
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        // Extract the path from the request URI relative to the base /api/v1/files/
        String relativePath = request.getRequestURI().substring(request.getContextPath().length() + "/api/v1/files/".length());

        Resource resource = fileStorageService.loadFileAsResource(relativePath)
                .orElseThrow(() -> {
                    logger.warn("File not found for download: {}", relativePath);
                    // This will be caught by GlobalExceptionHandler if ResourceNotFoundException is thrown
                    // Or return ResponseEntity.notFound().build(); directly
                    return new ir.hamqadam.core.exception.ResourceNotFoundException("File", "path", relativePath);
                });

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type for path: {}", relativePath);
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                // Use "attachment" instead of "inline" if you want to force download
                .body(resource);
    }
}