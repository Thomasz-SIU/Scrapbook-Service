package com.ThomasZemen.PhotoDump.Controller;

import com.ThomasZemen.PhotoDump.DTO.AlbumDTO;
import com.ThomasZemen.PhotoDump.Model.Album;
import com.ThomasZemen.PhotoDump.Service.AlbumService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@CrossOrigin(origins = "*")
public class AlbumController {
    @Autowired
    private AlbumService albumService;

    @PostMapping
    public ResponseEntity<Album> createAlbum(@Valid @RequestBody AlbumDTO albumDTO) {
        return ResponseEntity.ok(albumService.createAlbum(albumDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Album> getAlbumById(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAlbumById(id));
    }

    @GetMapping
    public ResponseEntity<List<Album>> getAllAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(albumService.getAllAlbums(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Album> updateAlbum(
            @PathVariable Long id,
            @Valid @RequestBody AlbumDTO albumDTO) {
        return ResponseEntity.ok(albumService.updateAlbum(id, albumDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id) {
        albumService.deleteAlbum(id);
        return ResponseEntity.noContent().build();
    }
}
