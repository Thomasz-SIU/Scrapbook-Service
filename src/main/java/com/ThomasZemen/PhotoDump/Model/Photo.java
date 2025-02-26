package com.ThomasZemen.PhotoDump.Model;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Photo {
    private Long id;
    private String title;
    private String description;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    private Album album;

    public static Photo fromResultSet(java.sql.ResultSet resultSet, Album album) throws java.sql.SQLException
    {
        Photo photo = new Photo();
        photo.setId(resultSet.getLong("id"));
        photo.setTitle(resultSet.getString("title"));
        photo.setDescription(resultSet.getString("description"));
        photo.setFileName(resultSet.getString("file_name"));
        photo.setContentType(resultSet.getString("content_type"));
        photo.setFileSize(resultSet.getLong("file_size"));
        photo.setUploadedAt(resultSet.getTimestamp("uploaded_at").toLocalDateTime());
        photo.setAlbum(album); //only need album ID for reference
        //TODO-SEE IF WE CAN REMOVE ALBUM OBJECT
        return photo;
    }
}
