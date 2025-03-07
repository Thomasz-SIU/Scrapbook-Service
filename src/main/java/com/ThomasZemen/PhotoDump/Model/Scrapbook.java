package com.ThomasZemen.PhotoDump.Model;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class Scrapbook {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    public static Scrapbook fromResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        Scrapbook scrapbook = new Scrapbook();
        scrapbook.setId(rs.getLong("id"));
        scrapbook.setTitle(rs.getString("title"));
        scrapbook.setDescription(rs.getString("description"));
        scrapbook.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        return scrapbook;
    };
}
