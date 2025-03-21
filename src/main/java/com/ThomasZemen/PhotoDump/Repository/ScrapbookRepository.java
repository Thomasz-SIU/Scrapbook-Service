package com.ThomasZemen.PhotoDump.Repository;

import com.ThomasZemen.PhotoDump.DAO.ScrapbookDAO;
import com.ThomasZemen.PhotoDump.Model.Scrapbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ScrapbookRepository implements ScrapbookDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Scrapbook save(Scrapbook scrapbook) {
        if(scrapbook.getId() == null)
        {
            String sql = "INSERT INTO scrapbook (title, description, created_at) VALUES (?,?,?)";
            KeyHolder key = new GeneratedKeyHolder();

            jdbcTemplate.update(connection->{
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); {
                    ps.setString(1, scrapbook.getTitle());
                    ps.setString(2, scrapbook.getDescription());
                    ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    return ps;
                }
            }, key);
            scrapbook.setId(key.getKey().longValue());
        }
        else
        {
            //Update existing
           String sql = "UPDATE scrapbook SET title = ?, description = ? WHERE id = ?";
           jdbcTemplate.update(sql, scrapbook.getTitle(), scrapbook.getDescription(), scrapbook.getId());
        }
        return scrapbook;
    }

    @Override
    public Optional<Scrapbook> findById(long id) {
        return Optional.empty();
        ///
    }

    @Override
    public List<Scrapbook> findAll() {
        return null;
    }

    @Override
    public Scrapbook update(Scrapbook scrapbook) {
        return null;
    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public List<Scrapbook> findByTitleContaining(String title, int offset, int limit) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }
}
