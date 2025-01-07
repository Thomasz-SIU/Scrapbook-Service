package com.ThomasZemen.PhotoDump.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Data
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String fileName;    // Store the actual file name

    private String contentType; // Store the image MIME type

    private Long fileSize;      // Store the file size in bytes

    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "album_id")
    @JsonBackReference
    private Album album;

    @PrePersist
    protected void onCreate()
    {
        uploadedAt = LocalDateTime.now();
    }

//    // Transient field for full URL
//    @Transient
//    public String getImageUrl() {
//        if (fileName == null) return null;
//        return "/uploads/" + fileName;
//    }
}
