package com.eCom.MediaService.service.strategy;

import com.eCom.Commons.utils.MediaUtils; // ✅ Import your utility
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface MediaStorageStrategy {
    Mono<String> store(FilePart filePart, String entityType, String mediaType);

    // ✅ Add 'default' so implementing classes don't throw errors
    default boolean isVideo(String fileName) {
        return MediaUtils.isVideo(fileName);
    }
}