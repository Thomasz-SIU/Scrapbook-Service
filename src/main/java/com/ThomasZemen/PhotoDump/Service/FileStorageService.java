package com.ThomasZemen.PhotoDump.Service;

import com.ThomasZemen.PhotoDump.Exception.PhotoAlbumException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path fileStorageLocation;

    public FileStorageService(@Value("${upload.path}") String uploadPath) {
        this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new PhotoAlbumException("Could not create upload directory!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null ?
                    originalFileName.substring(originalFileName.lastIndexOf(".")) : ".jpg";
            String fileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new PhotoAlbumException("Could not store file. Please try again!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new PhotoAlbumException("Could not delete file. Please try again!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}