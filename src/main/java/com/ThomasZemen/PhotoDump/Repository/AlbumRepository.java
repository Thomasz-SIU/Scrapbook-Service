package com.ThomasZemen.PhotoDump.Repository;

import com.ThomasZemen.PhotoDump.Model.Album;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
}
