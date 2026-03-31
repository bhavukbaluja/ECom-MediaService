package com.eCom.MediaService.controller;

import com.eCom.MediaService.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public")
public class MediaPublicController {


    private final ImageService imageService;

    public MediaPublicController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/getCarouselImages")
    public ResponseEntity<Map<String, Object>> getCarouselImages(@RequestParam("deviceType") String deviceType){
        return ResponseEntity.ok(imageService.getCarouselImages(deviceType));
    }
}
