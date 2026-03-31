package com.eCom.MediaService.service.strategy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageStrategy implements MediaStorageStrategy {

    @Override
    public Mono<String> store(FilePart filePart, String entityType, String mediaType) {
        String fileName = filePart.filename();

        // 1. Determine the root folder (images vs videos)
        String mediaFolder = isVideo(fileName) ? "videos" : "images";

        // 2. Normalize entityType (singular & lowercase as per your rule)
        String folderName = entityType.toLowerCase().trim();

        // 3. Construct the S3 "Key" (The full path inside the bucket)
        // Format: images/product/1711654321_shirt.jpg
        String s3Key = mediaFolder + "/" + folderName + "/" + System.currentTimeMillis() + "_" + fileName;

        // ✅ AWS S3 Logic would use this s3Key to upload
        // For now, returning the simulated URL
        String bucketName = "ecom-media-bucket";
        return Mono.just("https://" + bucketName + ".s3.amazonaws.com/" + s3Key);
    }
}