package com.ThomasZemen.PhotoDump.Service;

import com.ThomasZemen.PhotoDump.DAO.PhotoDAO;
import com.ThomasZemen.PhotoDump.DTO.PhotoDTO;
import com.ThomasZemen.PhotoDump.Exception.PhotoAlbumException;
import com.ThomasZemen.PhotoDump.Exception.ResourceNotFoundException;
import com.ThomasZemen.PhotoDump.Model.Album;
import com.ThomasZemen.PhotoDump.Model.Photo;
import com.ThomasZemen.PhotoDump.Repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PhotoService {
    private final PhotoDAO photoDAO;
    private final AlbumService albumService;
    private final FileStorageService fileStorageService;

    // Maximum file size (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Autowired
    public PhotoService(PhotoDAO photoDAO,
                        AlbumService albumService,
                        FileStorageService fileStorageService) {
        this.photoDAO = photoDAO;
        this.albumService = albumService;
        this.fileStorageService = fileStorageService;
    }

    public Photo savePhoto(MultipartFile file, PhotoDTO photoDTO) {
        validateFile(file);
        Album album = albumService.getAlbumById(photoDTO.getAlbumId());

        String fileName = fileStorageService.storeFile(file);

        Photo photo = new Photo();
        updatePhotoFromDTO(photo, photoDTO);
        photo.setAlbum(album);
        photo.setFileName(fileName);
        photo.setContentType(file.getContentType());
        photo.setFileSize(file.getSize());

        return photoDAO.save(photo);
    }

    public Photo getPhotoById(Long id) {
        return photoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found with id: " + id));
    }

    public List<Photo> getPhotosByAlbum(Long albumId, Pageable pageable) {
        // Verify album exists
        albumService.getAlbumById(albumId);
        return photoDAO.findByAlbumId(
                albumId,
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
    }

    public Photo updatePhoto(Long id, PhotoDTO photoDTO) {
        Photo photo = getPhotoById(id);
        Album newAlbum = albumService.getAlbumById(photoDTO.getAlbumId());

        // Check if album has changed
        if (!photo.getAlbum().getId().equals(newAlbum.getId())) {
            photo.setAlbum(newAlbum);
        }

        updatePhotoFromDTO(photo, photoDTO);
        return photoDAO.save(photo);
    }

    @Transactional
    public void deletePhoto(Long id) {
        Photo photo = getPhotoById(id);
        try {
            // First try to delete the physical file
            fileStorageService.deleteFile(photo.getFileName());
            // If successful, delete the database record
            photoDAO.delete(id);
        } catch (PhotoAlbumException e) {
            // If file deletion fails, throw exception to rollback transaction
            throw new PhotoAlbumException(
                    "Failed to delete photo file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new PhotoAlbumException("File cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (file.isEmpty()) {
            throw new PhotoAlbumException("Failed to store empty file", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new PhotoAlbumException("File content type cannot be determined", HttpStatus.BAD_REQUEST);
        }

        if (!contentType.startsWith("image/")) {
            throw new PhotoAlbumException(
                    "Invalid file type. Only image files are allowed: " + contentType,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new PhotoAlbumException(
                    String.format("File size %.2f MB exceeds maximum limit of %.2f MB",
                            file.getSize() / (1024.0 * 1024.0),
                            MAX_FILE_SIZE / (1024.0 * 1024.0)),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void updatePhotoFromDTO(Photo photo, PhotoDTO photoDTO) {
        photo.setTitle(photoDTO.getTitle());
        photo.setDescription(photoDTO.getDescription());
    }

    public List<Photo> searchPhotosByTitle(String title, Pageable pageable) {
        return photoDAO.findByTitleContaining(
                title,
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
    }

    public List<Photo> getPhotosByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (start == null || end == null) {
            throw new PhotoAlbumException("Start and end dates are required", HttpStatus.BAD_REQUEST);
        }

        if (start.isAfter(end)) {
            throw new PhotoAlbumException("Start date must be before end date", HttpStatus.BAD_REQUEST);
        }

        return photoDAO.findByUploadDateRange(
                start,
                end,
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
    }
}
