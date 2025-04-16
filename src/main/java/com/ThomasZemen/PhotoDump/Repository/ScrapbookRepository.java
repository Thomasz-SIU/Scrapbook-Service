package com.ThomasZemen.PhotoDump.Repository;

import com.ThomasZemen.PhotoDump.DAO.ScrapbookDAO;
import com.ThomasZemen.PhotoDump.Exception.ResourceNotFoundException;
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
        if (scrapbook.getId() == null){
            throw new IllegalArgumentException("You cannot save a scrapbook with an invalid id");
        }
        return null;

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
        String sql = "SELECT * FROM scrapbook WHERE id = ?";

        List<Scrapbook> scrapbooks = jdbcTemplate.query(sql,
                (rs,rowNum) -> Scrapbook.fromResultSet(rs), id);

        return scrapbooks.isEmpty() ? Optional.empty() : Optional.of(scrapbooks.get(0));
    }

    @Override
    public List<Scrapbook> findAll(int limit, int offset) {
        String sql = "SELECT * from scrapbook LIMIT ?, OFFSET ?";
        return jdbcTemplate.query(sql,
                (rs,rowNum) -> Scrapbook.fromResultSet(rs), limit, offset);
    }

    @Override
    public Scrapbook update(Scrapbook scrapbook) {
        if(scrapbook.getId() == null) throw new IllegalArgumentException("Scrapbook cannot be missing ID");

        String sql = "UPDATE scrapbook SET title = ?, description = ?, WHERE id = ?";
        int rowsUpdated = jdbcTemplate.update(sql,
                            scrapbook.getTitle(),
                            scrapbook.getDescription(),
                            scrapbook.getId());
        if(rowsUpdated == 0)
        {
            throw new ResourceNotFoundException("No scrapbook exists with id " + scrapbook.getId());
        }
        return scrapbook;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM scrapbook WHERE id = ?";
        jdbcTemplate.update(sql,id);
    }

    @Override
    public List<Scrapbook> findByTitleContaining(String title, int offset, int limit) { //NEEDS TO BE FIXED SINCE WE NEED TO CALL JOINS
        String sql = "SELECT * FROM scrapbook WHERE title = ?, OFFSET ?, LIMIT ?";
        return jdbcTemplate.query(sql,
                (rs, rowMap) -> Scrapbook.fromResultSet(rs), offset, limit);
    }

    @Override
    public long count() {
        return 0;
    }
}
