package com.ThomasZemen.PhotoDump.Repository;

import com.ThomasZemen.PhotoDump.DAO.AlbumDAO;
import com.ThomasZemen.PhotoDump.DAO.PhotoDAO;
import com.ThomasZemen.PhotoDump.Exception.PhotoAlbumException;
import com.ThomasZemen.PhotoDump.Exception.ResourceNotFoundException;
import com.ThomasZemen.PhotoDump.Model.Album;
import com.ThomasZemen.PhotoDump.Model.Photo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class PhotoRepository implements PhotoDAO {
    private final JdbcTemplate jdbcTemplate;
    private final AlbumDAO albumDAO;

    @Autowired
    public PhotoRepository(JdbcTemplate jdbcTemplate, AlbumDAO albumDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.albumDAO = albumDAO;
    }
    @Override
    public Photo update(Photo photo) {
        if (photo.getId() == null) {
            throw new IllegalArgumentException("Cannot update photo without ID");
        }

        String sql = "UPDATE photo SET title = ?, description = ?, album_id = ? WHERE id = ?";
        int updatedRows = jdbcTemplate.update(sql,
                photo.getTitle(),
                photo.getDescription(),
                photo.getAlbum().getId(),
                photo.getId());

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Photo not found with id: " + photo.getId());
        }

        return photo;
    }

    @Override
    public Photo save(Photo photo) {
        LocalDateTime now = LocalDateTime.now();
        if (photo.getId() == null) {
            String sql = "INSERT INTO photo (title, description, file_name, content_type, file_size, uploaded_at, album_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, photo.getTitle());
                ps.setString(2, photo.getDescription());
                ps.setString(3, photo.getFileName());
                ps.setString(4, photo.getContentType());
                ps.setLong(5, photo.getFileSize());
                ps.setTimestamp(6, Timestamp.valueOf(now));
                ps.setLong(7, photo.getAlbum().getId());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                photo.setId(key.longValue());
                photo.setUploadedAt(now);
            } else {
                throw new PhotoAlbumException("Failed to generate ID for new photo", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return update(photo);
        }
        return photo;
    }

    @Override
    public List<Photo> findByTitleContaining(String title, int offset, int limit) {
        String sql = "SELECT p.*, a.id as album_id, a.title as album_title, " +
                "a.description as album_description, a.created_at as album_created_at " +
                "FROM photo p " +
                "JOIN album a ON p.album_id = a.id " +
                "WHERE p.title LIKE ? " +
                "LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Album album = new Album();
            album.setId(rs.getLong("album_id"));
            album.setTitle(rs.getString("album_title"));
            album.setDescription(rs.getString("album_description"));
            album.setCreatedAt(rs.getTimestamp("album_created_at").toLocalDateTime());

            return Photo.fromResultSet(rs, album);
        }, "%" + title + "%", limit, offset);
    }

    @Override
    public Optional<Photo> findById(Long id) {
        String sql = "SELECT p.*, a.id as album_id, a.title as album_title, " +
                "a.description as album_description, a.created_at as album_created_at " +
                "FROM photo p " +
                "JOIN album a ON p.album_id = a.id " +
                "WHERE p.id = ?";

        List<Photo> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Album album = new Album();
            album.setId(rs.getLong("album_id"));
            album.setTitle(rs.getString("album_title"));
            album.setDescription(rs.getString("album_description"));
            album.setCreatedAt(rs.getTimestamp("album_created_at").toLocalDateTime());

            return Photo.fromResultSet(rs, album);
        }, id);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Photo> findByAlbumId(Long albumId, int offset, int limit) {
        String sql = "SELECT p.*, a.id as album_id, a.title as album_title, " +
                "a.description as album_description, a.created_at as album_created_at " +
                "FROM photo p " +
                "JOIN album a ON p.album_id = a.id " +
                "WHERE p.album_id = ? " +
                "LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Album album = new Album();
            album.setId(rs.getLong("album_id"));
            album.setTitle(rs.getString("album_title"));
            album.setDescription(rs.getString("album_description"));
            album.setCreatedAt(rs.getTimestamp("album_created_at").toLocalDateTime());

            return Photo.fromResultSet(rs, album);
        }, albumId, limit, offset);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM photo WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void deleteAlbumId(Long albumId) {
        String sql = "DELETE FROM photo WHERE album_id = ?";
        jdbcTemplate.update(sql, albumId);
    }

    @Override
    public long countByAlbumId(Long albumId) {
        String sql = "SELECT COUNT(*) FROM photo WHERE album_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, albumId);
    }

    @Override
    public List<Photo> findByUploadDateRange(LocalDateTime start, LocalDateTime end, int offset, int limit) {
        String sql = "SELECT p.*, a.id as album_id, a.title as album_title, " +
                "a.description as album_description, a.created_at as album_created_at " +
                "FROM photo p " +
                "JOIN album a ON p.album_id = a.id " +
                "WHERE p.uploaded_at BETWEEN ? AND ? " +
                "ORDER BY p.uploaded_at DESC " +
                "LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Album album = new Album();
            album.setId(rs.getLong("album_id"));
            album.setTitle(rs.getString("album_title"));
            album.setDescription(rs.getString("album_description"));
            album.setCreatedAt(rs.getTimestamp("album_created_at").toLocalDateTime());

            return Photo.fromResultSet(rs, album);
        }, Timestamp.valueOf(start), Timestamp.valueOf(end), limit, offset);
    }
}
