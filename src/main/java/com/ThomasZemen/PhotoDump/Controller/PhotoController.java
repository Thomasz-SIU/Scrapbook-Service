package com.ThomasZemen.PhotoDump.Controller;

import com.ThomasZemen.PhotoDump.DTO.PhotoDTO;
import com.ThomasZemen.PhotoDump.Model.Photo;
import com.ThomasZemen.PhotoDump.Service.PhotoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*")
public class PhotoController {
    @Autowired
    private PhotoService photoService;

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Photo> uploadPhoto(
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("albumId") String albumId) {
        PhotoDTO photoDTO = new PhotoDTO();
        photoDTO.setTitle(title);
        photoDTO.setDescription(description);
        photoDTO.setAlbumId(Long.parseLong(albumId)); //must
        return ResponseEntity.ok(photoService.savePhoto(file, photoDTO));
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<Photo>> getPhotosByAlbum(
            @PathVariable Long albumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(photoService.getPhotosByAlbum(albumId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Photo> getPhotoById(@PathVariable Long id) {
        return ResponseEntity.ok(photoService.getPhotoById(id));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable Long id) {
        Photo photo = photoService.getPhotoById(id);
        Path filePath = Paths.get(uploadPath).resolve(photo.getFileName());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(photo.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + photo.getFileName() + "\"")
                    .body(resource);
        } catch (IOException ex) {
            throw new RuntimeException("File not found", ex);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        photoService.deletePhoto(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Photo> updatePhoto(
            @PathVariable Long id,
            @Valid @RequestBody PhotoDTO photoDTO) {
        return ResponseEntity.ok(photoService.updatePhoto(id, photoDTO));
    }
}