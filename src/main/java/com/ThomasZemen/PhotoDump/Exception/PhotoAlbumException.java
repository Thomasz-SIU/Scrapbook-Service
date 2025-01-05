package com.ThomasZemen.PhotoDump.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PhotoAlbumException extends RuntimeException {
    private final HttpStatus status;

    public PhotoAlbumException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
