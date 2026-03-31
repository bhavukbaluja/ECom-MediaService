package com.eCom.MediaService.controller;

import com.eCom.MediaService.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.eCom.Commons.utils.MediaUtils.isVideo;

@RestController
@RequestMapping("/fetch")
public class MediaController {

    private final ImageService imageService;

    @Value("${shared.file.path}")
    private String sharedFilePath;

    public MediaController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{entityType}/{fileName}")
    public ResponseEntity<Resource> streamMedia(
            @PathVariable String entityType,
            @PathVariable String fileName) {

        try {
            String subDir = isVideo(fileName) ? "videos/" : "images/";
            Path filePath = Paths.get(sharedFilePath)
                    .resolve(subDir)
                    .resolve(entityType.toLowerCase())
                    .resolve(fileName)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // ✅ 1. Auto-detect content type (e.g., image/png, video/webm, etc.)
                MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                        .orElse(isVideo(fileName) ? MediaType.valueOf("video/mp4") : MediaType.IMAGE_JPEG);

                return ResponseEntity.ok()
                        .contentType(mediaType)
                        // ✅ 2. Performance: Cache for 365 days (reduces Mac CPU/Disk usage)
                        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        // ✅ 3. Streaming: Allow scrubbing/seeking in videos
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
