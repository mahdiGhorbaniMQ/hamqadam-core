package ir.hamqadam.core.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Optional;

public interface FileStorageService {

    /**
     * Stores the given file in a specified subdirectory.
     * Generates a unique filename to avoid collisions.
     *
     * @param file         The MultipartFile to store.
     * @param subDirectory The subdirectory within the base storage path (e.g., "avatars", "post-attachments").
     * @return The relative path (from the base storage path) or a unique identifier of the stored file.
     * @throws ir.hamqadam.core.exception.FileStorageException if storing fails.
     * @throws ir.hamqadam.core.exception.ValidationException if the file is invalid (e.g., empty, disallowed type).
     */
    String storeFile(MultipartFile file, String subDirectory);

    /**
     * Loads a file as a Spring Resource.
     *
     * @param filePath The relative path (from the base storage path) of the file to load.
     * @return An Optional containing the Resource if found.
     * @throws ir.hamqadam.core.exception.FileStorageException if loading fails or file not found.
     */
    Optional<Resource> loadFileAsResource(String filePath);

    /**
     * Deletes the specified file.
     *
     * @param filePath The relative path (from the base storage path) of the file to delete.
     * @return True if deletion was successful, false otherwise or if file did not exist.
     * @throws ir.hamqadam.core.exception.FileStorageException if deletion fails.
     */
    boolean deleteFile(String filePath);

    /**
     * Generates an externally accessible URL for the given file path.
     * For local storage, this might be a URL served by the application itself.
     * For cloud storage, this would be the direct URL from the cloud provider (e.g., S3 URL).
     *
     * @param filePath The relative path or identifier of the stored file.
     * @return The fully qualified URL to access the file.
     */
    String generateFileUrl(String filePath);

    /**
     * Gets the full path on the filesystem for a stored file.
     * Primarily for internal use or local filesystem strategy.
     *
     * @param filePath The relative path or identifier of the stored file.
     * @return The absolute Path object.
     */
    Path getFullPath(String filePath);
}