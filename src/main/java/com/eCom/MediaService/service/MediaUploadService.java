//package com.eCom.MediaService.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.codec.multipart.FilePart;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//import java.io.File;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//import static com.eCom.Commons.utils.MediaUtils.isVideo;
//
//
//@Service
//public class MediaUploadService {
//
//    @Value("${shared.file.path:/Users/bhavukbaluja/Coding/E-Com-Website/}")
//    private String sharedFilePath;
//
//    public Mono<String> uploadToDisk(FilePart filePart, String entityType) {
//        return Mono.defer(() -> {
//            String originalFilename = filePart.filename();
//            boolean isVideo = isVideo(originalFilename);
//
//            String baseDirName = isVideo ? "Shared/videos/" : "Shared/images/";
//            String relativePath = baseDirName + entityType.toLowerCase() + "/";
//            String finalPathDir = sharedFilePath + relativePath;
//            String filename = System.currentTimeMillis() + "_" + originalFilename;
//
//            Path filePath = Paths.get(finalPathDir).resolve(filename).normalize();
//
//            // Ensure directory exists
//            File dir = new File(finalPathDir);
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
//
//            return filePart.transferTo(filePath)
//                    .then(Mono.just(relativePath + filename));
//        }).subscribeOn(Schedulers.boundedElastic());
//    }
//
//}