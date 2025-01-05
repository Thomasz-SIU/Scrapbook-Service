package com.ThomasZemen.PhotoDump.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Photo> photos;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


}
