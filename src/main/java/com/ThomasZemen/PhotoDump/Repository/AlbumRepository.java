package com.ThomasZemen.PhotoDump.Repository;

import com.ThomasZemen.PhotoDump.DAO.AlbumDAO;
import com.ThomasZemen.PhotoDump.Model.Album;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public class AlbumRepository implements AlbumDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Album> findByTitleContaining(String title, int offset, int limit) {
        String sql = "SELECT * FROM album WHERE title LIKE ? LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> Album.fromResultSet(rs),
                "%" + title + "%", limit, offset);
    }
    @Override
    public long count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM album", Long.class);
    }

    @Override
    public Album save(Album album) {
        if (album.getId() == null) {
            // Insert new album
            String sql = "INSERT INTO album (title, description, created_at) VALUES (?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, album.getTitle());
                ps.setString(2, album.getDescription());
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, keyHolder);

            album.setId(keyHolder.getKey().longValue());
        } else {
            // Update existing album
            String sql = "UPDATE album SET title = ?, description = ? WHERE id = ?";
            jdbcTemplate.update(sql, album.getTitle(), album.getDescription(), album.getId());
        }
        return album;
    }

    @Override
    public List<Album> findEmptyAlbums() {
        String sql = "SELECT a.* FROM album a " +
                "LEFT JOIN photo p ON a.id = p.album_id " +
                "GROUP BY a.id " +
                "HAVING COUNT(p.id) = 0";
        return jdbcTemplate.query(sql, (rs, rowNum) -> Album.fromResultSet(rs));
    }

    @Override
    public List<Album> findByCreatedDateRange(LocalDateTime start, LocalDateTime end, int offset, int limit) {
        String sql = "SELECT * FROM album " +
                "WHERE created_at BETWEEN ? AND ? " +
                "ORDER BY created_at DESC " +
                "LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> Album.fromResultSet(rs),
                Timestamp.valueOf(start),
                Timestamp.valueOf(end),
                limit,
                offset);
    }

    @Override
    public Optional<Album> findById(Long id) {
        String sql = "SELECT * FROM album WHERE id = ?";
        List<Album> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> Album.fromResultSet(rs), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Album> findAll(int offset, int limit) {
        String sql = "SELECT * FROM album LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> Album.fromResultSet(rs), limit, offset);
    }

    @Override
    public Album update(Album album) {
        String sql = "UPDATE album SET title = ?, description = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                album.getTitle(),
                album.getDescription(),
                album.getId());
        return album;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM album WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM album WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
