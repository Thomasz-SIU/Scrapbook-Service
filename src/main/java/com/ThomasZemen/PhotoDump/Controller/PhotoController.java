package com.ThomasZemen.PhotoDump.Controller;

import com.ThomasZemen.PhotoDump.DTO.PhotoDTO;
import com.ThomasZemen.PhotoDump.Exception.PhotoAlbumException;
import com.ThomasZemen.PhotoDump.Model.Photo;
import com.ThomasZemen.PhotoDump.Service.PhotoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    @Autowired
    private final PhotoService photoService;
    private final Path fileStorageLocation;
    private static final Logger logger = LoggerFactory.getLogger(PhotoController.class);

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Autowired
    public PhotoController(
            PhotoService photoService,
            @Value("${upload.path}") String uploadPath) {
        this.photoService = photoService;
        this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    @CrossOrigin(
            origins = "${cors.allowed-origins}",
            allowedHeaders = {"Content-Type", "Authorization"},
            methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
            maxAge = 3600
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Photo> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("albumId") String albumId) {
        try {
            PhotoDTO photoDTO = new PhotoDTO();
            photoDTO.setTitle(title);
            photoDTO.setDescription(description);
            photoDTO.setAlbumId(Long.parseLong(albumId));

            Photo savedPhoto = photoService.savePhoto(file, photoDTO);
            return ResponseEntity.ok(savedPhoto);
        } catch (NumberFormatException e) {
            throw new PhotoAlbumException("Invalid album ID format", HttpStatus.BAD_REQUEST);
        }
    }

    @CrossOrigin(origins = "${cors.allowed-origins}")
    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<Photo>> getPhotosByAlbum(
            @PathVariable Long albumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePaginationParameters(page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(photoService.getPhotosByAlbum(albumId, pageable));
    }

    @CrossOrigin(origins = "${cors.allowed-origins}")
    @GetMapping("/{id}")
    public ResponseEntity<Photo> getPhotoById(@PathVariable Long id) {
        return ResponseEntity.ok(photoService.getPhotoById(id));
    }

    @CrossOrigin(origins = "${cors.allowed-origins}")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable Long id) {
        try {
            Photo photo = photoService.getPhotoById(id);
            Path filePath = fileStorageLocation.resolve(photo.getFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.error("Could not read file: {}", photo.getFileName());
                throw new PhotoAlbumException(
                        "Could not read file: " + photo.getFileName(),
                        HttpStatus.NOT_FOUND
                );
            }

            // Detect content type
            String contentType = determineContentType(photo.getContentType(), photo.getFileName());

            // Set cache control headers
            CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS)
                    .cachePublic()
                    .mustRevalidate();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(cacheControl)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.builder("inline")
                                    .filename(photo.getFileName())
                                    .build().toString())
                    .body(resource);

        } catch (IOException ex) {
            logger.error("Could not download file", ex);
            throw new PhotoAlbumException(
                    "Could not download file: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @CrossOrigin(origins = "${cors.allowed-origins}")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        photoService.deletePhoto(id);
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin(origins = "${cors.allowed-origins}")
    @PutMapping("/{id}")
    public ResponseEntity<Photo> updatePhoto(
            @PathVariable Long id,
            @Valid @RequestBody PhotoDTO photoDTO) {
        return ResponseEntity.ok(photoService.updatePhoto(id, photoDTO));
    }

    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw new PhotoAlbumException("Page number cannot be negative", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > 100) {
            throw new PhotoAlbumException("Page size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }
    }

    private String determineContentType(String originalContentType, String fileName) {
        if (originalContentType != null && !originalContentType.isEmpty()) {
            return originalContentType;
        }

        // Fallback to determination by file extension
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}