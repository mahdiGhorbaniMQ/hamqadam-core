package ir.hamqadam.core.service.impl;

import ir.hamqadam.core.exception.FileStorageException;
import ir.hamqadam.core.exception.ValidationException;
import ir.hamqadam.core.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final Path fileStorageLocation;
    private final String baseFileServeUrl;

    public FileStorageServiceImpl(@Value("${hamqadam.file-storage.upload-dir}") String uploadDir,
                                  @Value("${hamqadam.file-storage.base-serve-url:/api/v1/files}") String baseFileServeUrl) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseFileServeUrl = baseFileServeUrl;

        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("Created file storage directory: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            logger.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new FileStorageException("Could not create the directory for uploads.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new ValidationException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        try {
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            // Basic security check for filename
            if (originalFilename.contains("..")) {
                throw new ValidationException("Filename contains invalid path sequence: " + originalFilename);
            }

            // TODO: Add more validation: file size, allowed content types (MIME types)
            // Example:
            // if (file.getSize() > MAX_FILE_SIZE_BYTES) { throw new ValidationException("File size exceeds limit."); }
            // if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) { throw new ValidationException("Invalid file type."); }

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocationSubDirectory = this.fileStorageLocation;
            if (StringUtils.hasText(subDirectory)) {
                // Clean and resolve subdirectory path to prevent path traversal
                Path cleanSubDirectory = Paths.get(subDirectory).normalize();
                if (cleanSubDirectory.startsWith("..") || cleanSubDirectory.isAbsolute()) {
                    throw new ValidationException("Invalid subdirectory: " + subDirectory);
                }
                targetLocationSubDirectory = this.fileStorageLocation.resolve(cleanSubDirectory);
                Files.createDirectories(targetLocationSubDirectory); // Ensure subdirectory exists
            }

            Path targetLocation = targetLocationSubDirectory.resolve(uniqueFileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Stored file: {} successfully at {}", originalFilename, targetLocation);

                // Return the relative path including subdirectory for URL generation or DB storage
                if (StringUtils.hasText(subDirectory)) {
                    return Paths.get(subDirectory).resolve(uniqueFileName).toString().replace("\\", "/");
                } else {
                    return uniqueFileName;
                }
            } catch (IOException ex) {
                logger.error("Could not store file {}. Please try again!", originalFilename, ex);
                throw new FileStorageException("Could not store file " + originalFilename + ". Please try again!", ex);
            }

        } catch (FileStorageException | ValidationException ex) {
            throw ex; // Re-throw specific exceptions
        } catch (Exception ex) {
            logger.error("An unexpected error occurred while storing file {}", originalFilename, ex);
            throw new FileStorageException("Could not store file " + originalFilename + " due to an unexpected error.", ex);
        }
    }

    @Override
    public Optional<Resource> loadFileAsResource(String relativeFilePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(relativeFilePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return Optional.of(resource);
            } else {
                logger.warn("File not found or not readable: {}", relativeFilePath);
                return Optional.empty();
                // Consider throwing ResourceNotFoundException("File", "path", relativeFilePath) here
            }
        } catch (MalformedURLException ex) {
            logger.error("Malformed URL for file path: {}", relativeFilePath, ex);
            throw new FileStorageException("File not found (malformed URL): " + relativeFilePath, ex);
        }
    }

    @Override
    public boolean deleteFile(String relativeFilePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(relativeFilePath).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted file: {}", relativeFilePath);
                return true;
            } else {
                logger.warn("Attempted to delete non-existent file: {}", relativeFilePath);
                return false;
            }
        } catch (IOException ex) {
            logger.error("Could not delete file: {}", relativeFilePath, ex);
            throw new FileStorageException("Could not delete file " + relativeFilePath + ". Please try again!", ex);
        }
    }

    @Override
    public Path getFullPath(String relativeFilePath) {
        return this.fileStorageLocation.resolve(relativeFilePath).normalize();
    }


    @Override
    public String generateFileUrl(String relativeFilePath) {
        if (relativeFilePath == null) {
            return null;
        }
        // This assumes files are served by the application itself through a dedicated controller endpoint
        // The endpoint would use loadFileAsResource to get the file.
        // Example: If you have a FileController mapped to "/api/v1/files/{subdirectory}/{filename:.+}"
        // then the URL would be constructed like this.
        // For S3 or other cloud storage, this method would return the direct cloud URL.

        // Normalize path to use forward slashes for URL consistency
        String normalizedPath = relativeFilePath.replace("\\", "/");

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(this.baseFileServeUrl + "/") // Ensure baseFileServeUrl starts with /
                .path(normalizedPath)
                .toUriString();
    }
}