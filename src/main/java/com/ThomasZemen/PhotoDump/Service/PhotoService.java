package com.ThomasZemen.PhotoDump.Service;

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

import java.util.List;

@Service
@Transactional
public class PhotoService {
    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private FileStorageService fileStorageService;

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

        return photoRepository.save(photo);
    }

    public Photo getPhotoById(Long id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found with id: " + id));
    }

    public List<Photo> getPhotosByAlbum(Long albumId, Pageable pageable) {
        // Verify album exists
        albumService.getAlbumById(albumId);
        return photoRepository.findByAlbumId(albumId, pageable);
    }

    public Photo updatePhoto(Long id, PhotoDTO photoDTO) {
        Photo photo = getPhotoById(id);
        Album newAlbum = albumService.getAlbumById(photoDTO.getAlbumId());

        updatePhotoFromDTO(photo, photoDTO);
        photo.setAlbum(newAlbum);

        return photoRepository.save(photo);
    }

    public void deletePhoto(Long id) {
        Photo photo = getPhotoById(id);
        fileStorageService.deleteFile(photo.getFileName());
        photoRepository.deleteById(id);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty() || file == null) {
            throw new PhotoAlbumException("Failed to store empty file", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new PhotoAlbumException("File must be an image", HttpStatus.BAD_REQUEST);
        }

        // 10MB max file size
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new PhotoAlbumException("File size exceeds maximum limit of 10MB", HttpStatus.BAD_REQUEST);
        }
    }

    private void updatePhotoFromDTO(Photo photo, PhotoDTO photoDTO) {
        photo.setTitle(photoDTO.getTitle());
        photo.setDescription(photoDTO.getDescription());
    }
}
