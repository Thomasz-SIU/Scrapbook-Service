package com.ThomasZemen.PhotoDump.Exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends PhotoAlbumException{
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
