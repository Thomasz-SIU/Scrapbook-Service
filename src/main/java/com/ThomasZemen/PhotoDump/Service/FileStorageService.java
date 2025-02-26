package com.ThomasZemen.PhotoDump.Service;

import com.ThomasZemen.PhotoDump.Exception.PhotoAlbumException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.HashSet;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path fileStorageLocation;
    private final Set<String> allowedFileExtensions;
    private final ConcurrentMap<String, Long> fileUploadTimestamps;
    private static final long TEMP_FILE_TTL = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    public FileStorageService(
            @Value("${upload.path}") String uploadPath,
            @Value("${upload.allowed-extensions:jpg,jpeg,png,gif,webp}") String allowedExtensions) {
        this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.allowedFileExtensions = new HashSet<>();
        this.fileUploadTimestamps = new ConcurrentHashMap<>();

        // Initialize allowed file extensions
        for (String ext : allowedExtensions.split(",")) {
            this.allowedFileExtensions.add(ext.trim().toLowerCase());
        }
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(fileStorageLocation);

            // Set directory permissions (POSIX)
            try {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
                Files.setPosixFilePermissions(fileStorageLocation, perms);
            } catch (UnsupportedOperationException e) {
                logger.warn("POSIX file permissions not supported on this system");
            }

            validateStorageDirectory();
        } catch (IOException ex) {
            throw new PhotoAlbumException("Could not create upload directory!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateStorageDirectory() {
        if (!Files.isDirectory(fileStorageLocation)) {
            throw new PhotoAlbumException(
                    "Upload path is not a directory: " + fileStorageLocation,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (!Files.isWritable(fileStorageLocation)) {
            throw new PhotoAlbumException(
                    "Upload directory is not writable: " + fileStorageLocation,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public String storeFile(MultipartFile file) {
        validateFile(file);

        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new PhotoAlbumException("Invalid file name", HttpStatus.BAD_REQUEST);
            }

            // Get file extension and validate
            String fileExtension = getFileExtension(originalFileName);
            if (!allowedFileExtensions.contains(fileExtension.toLowerCase())) {
                throw new PhotoAlbumException(
                        "File type not allowed. Allowed types: " + String.join(", ", allowedFileExtensions),
                        HttpStatus.BAD_REQUEST
                );
            }

            // Generate unique filename
            String fileName = generateUniqueFileName(fileExtension);
            Path targetLocation = fileStorageLocation.resolve(fileName);

            // Copy file with ATOMIC_MOVE option if possible
            try {
                Path tempFile = Files.createTempFile(fileStorageLocation, "temp-", null);
                Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempFile, targetLocation, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback to normal copy if atomic move is not supported
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Set file permissions (POSIX)
            try {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r-----");
                Files.setPosixFilePermissions(targetLocation, perms);
            } catch (UnsupportedOperationException e) {
                logger.warn("POSIX file permissions not supported on this system");
            }

            fileUploadTimestamps.put(fileName, System.currentTimeMillis());
            return fileName;

        } catch (IOException ex) {
            logger.error("Failed to store file", ex);
            throw new PhotoAlbumException(
                    "Failed to store file: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            validateFilePath(filePath);

            if (!Files.deleteIfExists(filePath)) {
                logger.warn("File not found for deletion: {}", fileName);
                throw new PhotoAlbumException("File not found: " + fileName, HttpStatus.NOT_FOUND);
            }

            fileUploadTimestamps.remove(fileName);
            logger.info("Successfully deleted file: {}", fileName);

        } catch (IOException ex) {
            logger.error("Error deleting file: {}", fileName, ex);
            throw new PhotoAlbumException(
                    "Could not delete file: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new PhotoAlbumException("File cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (file.isEmpty()) {
            throw new PhotoAlbumException("File is empty", HttpStatus.BAD_REQUEST);
        }

        // Additional security checks can be added here
        // e.g., virus scanning, file type verification, etc.
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new PhotoAlbumException("File must have an extension", HttpStatus.BAD_REQUEST);
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private void validateFilePath(Path filePath) {
        if (!filePath.normalize().startsWith(fileStorageLocation.normalize())) {
            throw new PhotoAlbumException("Invalid file path", HttpStatus.BAD_REQUEST);
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour a cleanup
    public void cleanupTempFiles() {
        logger.info("Starting temporary file cleanup");
        long currentTime = System.currentTimeMillis();

        fileUploadTimestamps.forEach((fileName, uploadTime) -> {
            if (currentTime - uploadTime > TEMP_FILE_TTL) {
                Path filePath = fileStorageLocation.resolve(fileName);
                try {
                    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                        Files.delete(filePath);
                        fileUploadTimestamps.remove(fileName);
                        logger.info("Cleaned up temporary file: {}", fileName);
                    }
                } catch (IOException e) {
                    logger.error("Error cleaning up temporary file: {}", fileName, e);
                }
            }
        });
    }

    public void cleanup() {
        try {
            FileSystemUtils.deleteRecursively(fileStorageLocation);
            Files.createDirectories(fileStorageLocation);
            logger.info("Storage cleanup completed successfully");
        } catch (IOException e) {
            logger.error("Error during storage cleanup", e);
            throw new PhotoAlbumException(
                    "Failed to cleanup storage: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}