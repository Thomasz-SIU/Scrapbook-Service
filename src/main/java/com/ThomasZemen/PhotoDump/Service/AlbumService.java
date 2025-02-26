package com.ThomasZemen.PhotoDump.Service;

import com.ThomasZemen.PhotoDump.DAO.AlbumDAO;
import com.ThomasZemen.PhotoDump.DAO.PhotoDAO;
import com.ThomasZemen.PhotoDump.DTO.AlbumDTO;
import com.ThomasZemen.PhotoDump.Exception.ResourceNotFoundException;
import com.ThomasZemen.PhotoDump.Model.Album;
import com.ThomasZemen.PhotoDump.Model.Photo;
import com.ThomasZemen.PhotoDump.Repository.AlbumRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.hibernate.internal.util.collections.BoundedConcurrentHashMap.EvictionPolicy.MAX_BATCH_SIZE;

@Service
@Transactional
public class AlbumService {
    private final AlbumDAO albumDAO;
    private final PhotoDAO photoDAO;
    private final FileStorageService fileStorageService;
    private static final Logger logger = LoggerFactory.getLogger(AlbumService.class);


    @Autowired
    public AlbumService(AlbumDAO albumDAO, PhotoDAO photoDAO, FileStorageService fileStorageService) {
        this.albumDAO = albumDAO;
        this.photoDAO = photoDAO;
        this.fileStorageService = fileStorageService;
    }

    public Album createAlbum(AlbumDTO albumDTO) {
        Album album = new Album();
        updateAlbumFromDTO(album, albumDTO);
        return albumDAO.save(album);
    }

    public Album getAlbumById(Long id) {
        return albumDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));
    }

    public List<Album> getAllAlbums(Pageable pageable) {
        return albumDAO.findAll(
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
    }

    public Album updateAlbum(Long id, AlbumDTO albumDTO) {
        Album album = getAlbumById(id);
        updateAlbumFromDTO(album, albumDTO);
        return albumDAO.save(album);
    }

    @Transactional
    public void deleteAlbum(Long id) {
        int offset = 0;

        while (true) {
            // Get photos in batches
            List<Photo> photos = photoDAO.findByAlbumId(id, offset, MAX_BATCH_SIZE);
            if (photos.isEmpty()) {
                break;
            }

            // Delete files for current batch
            for (Photo photo : photos) {
                try {
                    fileStorageService.deleteFile(photo.getFileName());
                } catch (Exception e) {
                    // Log error but continue with deletion
                    logger.error("Failed to delete file for photo {}: {}", photo.getId(), e.getMessage());
                }
            }

            offset += photos.size();
        }

        // Delete all photos from the database
        photoDAO.deleteAlbumId(id);

        // Finally delete the album
        albumDAO.delete(id);
    }

    private void updateAlbumFromDTO(Album album, AlbumDTO albumDTO) {
        album.setTitle(albumDTO.getTitle());
        album.setDescription(albumDTO.getDescription());
    }

    public List<Album> getEmptyAlbums() {
        return albumDAO.findEmptyAlbums();
    }

    public List<Album> searchAlbumsByTitle(String title, Pageable pageable) {
        return albumDAO.findByTitleContaining(
                title,
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
    }
}
