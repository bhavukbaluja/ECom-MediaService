package com.eCom.MediaService.service.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageStrategy implements MediaStorageStrategy {

    @Value("${shared.file.path}")
    private String sharedFilePath;

    @Override
    public Mono<String> store(FilePart filePart, String entityType, String mediaType) {

            String originalFilename = filePart.filename();
            String subDir = "/Shared/" + entityType.toLowerCase() + "/";

            // Determine the media sub-folder
            String folder;
            if (mediaType != null) {
                String lowerMedia = mediaType.toLowerCase();
                // Only add 's' if it doesn't already end with 's'
                folder = lowerMedia.endsWith("s") ? lowerMedia : lowerMedia + "s";
            } else if (isVideo(originalFilename)) {
                folder = "videos";
            } else {
                folder = "images";
            }

            String relativePath = subDir + folder + "/";
            String filename = System.currentTimeMillis() + "_" + originalFilename;

            Path targetDir = Paths.get(sharedFilePath, relativePath).normalize();
            Path targetFile = targetDir.resolve(filename);

            // 1. First, ensure the directory exists asynchronously
            return Mono.fromCallable(() -> {
                        File dir = targetDir.toFile();
                        if (!dir.exists()) dir.mkdirs();
                        return targetFile;
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(path -> {
                        // ✅ Explicitly log the start to see if it even reaches this point
                        System.out.println("💾 Starting disk write for: " + filePart.filename());

                        return filePart.transferTo(path)
                                .checkpoint("Disk-Write-Checkpoint") // Helps identify where it hangs
                                .then(Mono.just(relativePath + filename));
                    })
                    // 🛡️ If the disk write takes > 30s for 8MB, kill it so it doesn't hang the thread
                    .timeout(Duration.ofSeconds(60))
                    .doOnSuccess(s -> System.out.println("✅ Completed: " + filePart.filename()));
    }
}