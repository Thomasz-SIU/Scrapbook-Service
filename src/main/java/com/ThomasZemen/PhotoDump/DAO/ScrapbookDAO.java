package com.ThomasZemen.PhotoDump.DAO;

import com.ThomasZemen.PhotoDump.Model.Scrapbook;

import java.util.List;
import java.util.Optional;

public interface ScrapbookDAO {
    Scrapbook save(Scrapbook scrapbook);
    Optional<Scrapbook> findById(long id);
    List<Scrapbook> findAll();
    Scrapbook update(Scrapbook scrapbook);
    void delete(Long id);
    List<Scrapbook> findByTitleContaining(String title,int offset, int limit);
    long count();

}
