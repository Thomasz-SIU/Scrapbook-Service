package com.ThomasZemen.PhotoDump.Service;

import com.ThomasZemen.PhotoDump.DTO.AlbumDTO;
import com.ThomasZemen.PhotoDump.Exception.ResourceNotFoundException;
import com.ThomasZemen.PhotoDump.Model.Album;
import com.ThomasZemen.PhotoDump.Repository.AlbumRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class AlbumService {
    @Autowired
    private AlbumRepository albumRepository;

    public Album createAlbum(AlbumDTO albumDTO) {
        Album album = new Album();
        updateAlbumFromDTO(album, albumDTO);
        return albumRepository.save(album);
    }

    public Album getAlbumById(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));
    }

    public List<Album> getAllAlbums(Pageable pageable) {
        return albumRepository.findAll(pageable).getContent();
    }

    public Album updateAlbum(Long id, AlbumDTO albumDTO) {
        Album album = getAlbumById(id);
        updateAlbumFromDTO(album, albumDTO);
        return albumRepository.save(album);
    }

    public void deleteAlbum(Long id) {
        if (!albumRepository.existsById(id)) {
            throw new ResourceNotFoundException("Album not found with id: " + id);
        }
        albumRepository.deleteById(id);
    }

    private void updateAlbumFromDTO(Album album, AlbumDTO albumDTO) {
        album.setTitle(albumDTO.getTitle());
        album.setDescription(albumDTO.getDescription());
    }
}
