package com.eCom.MediaService.service;

import com.eCom.MediaService.service.strategy.MediaStorageStrategy;
import org.springframework.stereotype.Service;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

@Service
public class MediaStorageService {

    private final MediaStorageStrategy storageStrategy;

    public MediaStorageService(MediaStorageStrategy storageStrategy) {
        this.storageStrategy = storageStrategy;
    }

    public Mono<String> upload(FilePart file, String entityType, String mediaType) {
        String normalizedEntity = entityType.toLowerCase().trim();

        return storageStrategy.store(file, normalizedEntity, mediaType);
    }
}