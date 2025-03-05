package com.ThomasZemen.PhotoDump.DAO;

import com.ThomasZemen.PhotoDump.Model.Album;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlbumDAO {

    Album save(Album album);
    Optional<Album> findById(Long id);
    List<Album> findAll(int offset, int limit);
    Album update(Album album);
    void delete(Long id);
    boolean existsById(Long id);

    List<Album> findByTitleContaining(String title, int offset, int limit);
    long count();
    List<Album> findEmptyAlbums();
    List<Album> findByCreatedDateRange(LocalDateTime start, LocalDateTime stop, int offset, int limit);
}
