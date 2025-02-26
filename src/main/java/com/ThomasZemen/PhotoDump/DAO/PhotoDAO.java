package com.ThomasZemen.PhotoDump.DAO;

import com.ThomasZemen.PhotoDump.Model.Photo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PhotoDAO {

    Photo save(Photo photo);
    Optional<Photo> findById(Long id);
    List<Photo> findByAlbumId(Long albumId, int offset, int limit);
    Photo update(Photo photo);
    void delete(Long id);

    void deleteAlbumId(Long albumId);

    long countByAlbumId(Long albumId);
    List<Photo> findByTitleContaining(String title, int offset, int limit);
    List<Photo> findByUploadDateRange(LocalDateTime start, LocalDateTime end, int offset, int limit);
}
