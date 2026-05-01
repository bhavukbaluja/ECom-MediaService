package com.eCom.MediaService.service;

import com.eCom.Commons.model.CropImageRequest;
import com.eCom.Commons.model.dtos.MediaDTO;
import com.eCom.Commons.model.panel.EntityMedia;
import com.eCom.Commons.model.settings.WebCarouselImage;
import com.eCom.Commons.repository.EntityMediaRepository;
import com.eCom.Commons.repository.WebCarouselImageRepository;
import com.eCom.Commons.utils.MediaUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.eCom.Commons.utils.MediaUtils.extractFileName;
import static com.eCom.Commons.utils.MediaUtils.reconstructMediaPaths;

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${shared.file.path}")
    private String sharedFilePath;

    private final WebCarouselImageRepository webCarouselImageRepository;
    private final MediaUtils mediaUtils;
    private final EntityMediaRepository entityMediaRepository;

    public Mono<ResponseEntity<String>> saveCroppedImg(CropImageRequest cropRequest) throws java.io.IOException {
        try {
            File imageFile = new File(sharedFilePath, cropRequest.getUrl());
            if (!imageFile.exists()) {
                return Mono.just(ResponseEntity.notFound().build());
            }

            BufferedImage originalImage = null;
            try {
                originalImage = ImageIO.read(imageFile);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            int x = cropRequest.getX1();
            int y = cropRequest.getY1();
            int width = cropRequest.getW();
            int height = cropRequest.getH();

            // Validate dimensions
            if (x < 0 || y < 0 || x + width > originalImage.getWidth() || y + height > originalImage.getHeight()) {
                return Mono.just(ResponseEntity.badRequest().body("Invalid crop dimensions."));
            }
            // ✂️ Perform the crop
            BufferedImage cropped = originalImage.getSubimage(
                    x,y,width,height
            );

            // 💾 Save cropped image
            File outputfile = new File(sharedFilePath, cropRequest.getUrl());
            try {
                String filename = Paths.get(cropRequest.getUrl()).getFileName().toString();;
                String fileExtension = filename.substring(filename.lastIndexOf('.') + 1);
                ImageIO.write(cropped, fileExtension, outputfile);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return Mono.just(ResponseEntity.ok()
                    .body("Image cropped and saved successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String saveCarouselImages(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String deviceType = entry.getKey(); // "pc" or "mobile"
            Object value = entry.getValue();

            if (value instanceof List<?>) {
                // ✅ 1. Map Device Type to a Virtual Entity ID
                // We use 0 for PC and 1 for Mobile (or any unique long constants)
                Long virtualId = deviceType.equalsIgnoreCase("pc") ? 0L : 1L;

                // ✅ 2. Clean and Extract FileNames
                List<MediaDTO> cleanedMedia = ((List<?>) value).stream()
                        .filter(obj -> obj instanceof Map)
                        .map(obj -> {
                            Map<String, Object> m = (Map<String, Object>) obj;
                            return MediaDTO.builder()
                                    .url(extractFileName((String) m.get("url")))
                                    .thumbnail(extractFileName((String) m.get("thumbnail")))
                                    .type((String) m.get("type"))
                                    .build();
                        }).toList();

                // ✅ 3. Sync into the standard EntityMedia table
                // We use a specific entity type constant "CAROUSEL"
                mediaUtils.syncEntityMedia(virtualId, "HOME_PAGE_CAROUSEL", cleanedMedia);
            }
        }
        return "Carousel images synced successfully in DB.";
    }

    public Map<String, Object> getCarouselImages(String deviceType) {
        Map<String, Object> dataMap = new HashMap<>();

        // Define which virtual IDs to fetch
        Map<String, Long> typesToFetch = new HashMap<>();
        if (deviceType.equalsIgnoreCase("pc") || deviceType.equalsIgnoreCase("all")) {
            typesToFetch.put("pc", 0L);
        }
        if (deviceType.equalsIgnoreCase("mobile") || deviceType.equalsIgnoreCase("all")) {
            typesToFetch.put("mobile", 1L);
        }

        for (Map.Entry<String, Long> entry : typesToFetch.entrySet()) {
            // ✅ Fetch from the standard repository
            List<EntityMedia> mediaEntries = entityMediaRepository
                    .findByEntityTypeAndEntityIdOrderByPositionAsc("HOME_PAGE_CAROUSEL", entry.getValue());

            List<MediaDTO> mediaDTOList = mediaEntries.stream()
                    .map(img -> {
                        // ✅ Use your reconstruction method
                        // It will use img.getEntityType() (which is "CAROUSEL") to build the path
                        Map<String, String> paths = reconstructMediaPaths(img);

                        return MediaDTO.builder()
                                .url(paths.get("mediaUrl"))
                                .thumbnail(paths.get("thumbnailUrl"))
                                .type(img.getType())
                                .build();
                    })
                    .toList();

            if (!mediaDTOList.isEmpty()) {
                dataMap.put(entry.getKey(), mediaDTOList);
            }
        }

        if (dataMap.isEmpty()) {
            throw new IllegalArgumentException("No carousel data found for: " + deviceType);
        }

        return Map.of("data", dataMap);
    }


}
