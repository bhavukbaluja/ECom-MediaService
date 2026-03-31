package com.eCom.MediaService.service;

import com.eCom.Commons.model.CropImageRequest;
import com.eCom.Commons.model.settings.WebCarouselImage;
import com.eCom.Commons.repository.WebCarouselImageRepository;
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

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${shared.file.path}")
    private String sharedFilePath;

    private final WebCarouselImageRepository webCarouselImageRepository;

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
        webCarouselImageRepository.deleteAll();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String deviceType = entry.getKey(); // "pc" or "mobile"
            Object value = entry.getValue();

            if (value instanceof List<?>) {
                List<?> imageList = (List<?>) value;

                for (Object imageObj : imageList) {
                    if (imageObj instanceof String imageName) {
                        WebCarouselImage webCarouselImage = new WebCarouselImage();
                        webCarouselImage.setDeviceType(deviceType);
                        webCarouselImage.setImageName(imageName);
                        webCarouselImageRepository.save(webCarouselImage);
                    }
                }
            }
        }

        return "Carousel images saved successfully.";
    }

    public Map<String, Object> getCarouselImages(String deviceType){
        Map<String, Object> map= new HashMap<>();
        if(deviceType.equalsIgnoreCase("pc")){
            map.put("data",webCarouselImageRepository.findAllByDeviceTypeIgnoreCase("pc")
                    .stream()
                    .map(WebCarouselImage::getImageName)
                    .toList());
        } else if(deviceType.equalsIgnoreCase("mobile")){
            map.put("data",webCarouselImageRepository.findAllByDeviceTypeIgnoreCase("mobile")
                    .stream()
                    .map(WebCarouselImage::getImageName)
                    .toList());
        }
        else if(deviceType.equalsIgnoreCase("all")){
            Map<String, Object> map2 = new HashMap<>();
            List<WebCarouselImage> pcImages = webCarouselImageRepository.findAllByDeviceTypeIgnoreCase("pc");
            map2.put("pc",pcImages
                    .stream()
                    .map(WebCarouselImage::getImageName)
                    .toList());
            map2.put("mobile",webCarouselImageRepository.findAllByDeviceTypeIgnoreCase("mobile")
                    .stream()
                    .map(WebCarouselImage::getImageName)
                    .toList());
            map.put("data", map2);
        }
        else {
            throw new IllegalArgumentException("Invalid device type: " + deviceType);
        }
        return map;
    }


}
