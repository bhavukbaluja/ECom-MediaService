package com.eCom.MediaService.controller;

import com.eCom.Commons.model.CropImageRequest;
import com.eCom.MediaService.model.record.MediaResponse;
import com.eCom.MediaService.service.ImageService;
import com.eCom.MediaService.service.MediaStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/management")
public class MediaManagementController {

    private final ImageService imageService;
    // ✅ Change: Use the StorageService (The "Traffic Controller")
    private final MediaStorageService storageService;

    public MediaManagementController(ImageService imageService, MediaStorageService storageService) {
        this.imageService = imageService;
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<MediaResponse>> upload(
            @RequestPart("uploadedFile") FilePart file,
            @RequestParam String entityType,
            @RequestParam(required = false) String mediaType // ✅ Make this optional
    ) {
        // Logic to handle logging based on presence of mediaType
        if (mediaType != null) {
            System.out.println("📥 Receiving file: " + file.filename() + " for " + entityType + " as " + mediaType);
        } else {
            System.out.println("📥 Receiving file: " + file.filename() + " for " + entityType);
        }

        // Both cases now call the same storageService method
        return storageService.upload(file, entityType, mediaType)
                .map(path -> {
                    System.out.println("✅ Successfully stored at: " + path);
                    return ResponseEntity.ok(MediaResponse.ok(path));
                })
                .doOnError(e -> System.err.println("🔥 Controller Error: " + e.getMessage()))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(MediaResponse.error("Upload failed: " + e.getMessage()))
                ));
    }

    @PostMapping("/cropimage")
    public Mono<ResponseEntity<String>> cropImage(@RequestBody CropImageRequest cropRequest) {
        try {
            // 🔁 Load image from local path (replace with your image path or logic)
            return imageService.saveCroppedImg(cropRequest);
        } catch (io.jsonwebtoken.io.IOException | RasterFormatException e) {
            e.printStackTrace();
            return Mono.just(ResponseEntity.internalServerError()
                    .body("Failed to crop image: " + e.getMessage()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/saveCarouselImages")
    public ResponseEntity<String> saveCarouselImages(@RequestBody Map<String, Object> map) {
        return ResponseEntity.ok(imageService.saveCarouselImages(map));
    }
}
