package com.ThomasZemen.PhotoDump.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Album {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    public static Album fromResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        Album album = new Album();
        album.setId(rs.getLong("id"));
        album.setTitle(rs.getString("title"));
        album.setDescription(rs.getString("description"));
        album.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return album;
    }

}
