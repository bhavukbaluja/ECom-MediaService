package com.eCom.MediaService.service.cleanup;

import com.eCom.Commons.audit.GenericAuditProducer;
import com.eCom.Commons.utils.AuditTrailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class ImageCleanupService {

    private final WebClient gatewayWebClient;
    private final AuditTrailUtils auditTrailUtils;

    @Value("${shared.file.path}")
    private String sharedFilePath;

    public ImageCleanupService(WebClient.Builder webClientBuilder,
                               GenericAuditProducer genericAuditProducer) {
        // Points to Gateway/Backend to fetch the "Source of Truth" for used files
        this.gatewayWebClient = webClientBuilder.baseUrl("lb://APIGateway").build();
        this.auditTrailUtils = new AuditTrailUtils(genericAuditProducer);
    }

    @Scheduled(fixedDelayString = "#{${cleanup.images} * 60 * 60 * 1000}")
    public void performGlobalMediaCleanup() {
        log.info("🧹 Starting dynamic system-wide media cleanup...");

        try {
            List<String> activePaths = gatewayWebClient.get()
                    .uri("/api/internal/active-media-list")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .block();

            if (activePaths == null || activePaths.isEmpty()) {
                log.warn("⚠️ Aborting: Keep-list is empty.");
                return;
            }

            int totalDeleted = scanAndDeleteRecursively(new File(sharedFilePath), "", activePaths);

            if (totalDeleted > 0) {
                log.info("✅ Cleanup successful. Deleted {} files.", totalDeleted);
                publishCleanupAudit(totalDeleted);
            }
        } catch (Exception e) {
            log.error("🔥 Error during media cleanup: {}", e.getMessage());
        }
    }

    /**
     * Recursively walks through the directory tree.
     * @param currentDir The physical directory being scanned.
     * @param relativePath The path accumulated so far (e.g., "/product/images").
     * @param keepList The master list from the Backend.
     */
    private int scanAndDeleteRecursively(File currentDir, String relativePath, List<String> keepList) {
        if (!currentDir.exists() || !currentDir.isDirectory()) return 0;

        File[] entries = currentDir.listFiles();
        if (entries == null) return 0;

        int deletedCount = 0;
        for (File entry : entries) {
            // Reconstruct the path as it would appear in the DB
            // e.g., "" + "/" + "product" -> "/product"
            String currentRelativePath = relativePath + "/" + entry.getName();

            if (entry.getName().equalsIgnoreCase("temp")) continue;
            else if (entry.isDirectory()) {
                // If it's a folder, dive deeper
                deletedCount += scanAndDeleteRecursively(entry, currentRelativePath, keepList);
            } else if (entry.isFile()) {
                // If it's a file, check if its relative path is in our 'Keep List'
                if (!keepList.contains(currentRelativePath)) {
                    if (entry.delete()) {
                        log.debug("🗑️ Deleted: {}", currentRelativePath);
                        deletedCount++;
                    }
                }
            }
        }
        // Optional: Add this at the very end of the method, after the for-loop
        if (currentDir.listFiles().length == 0 && !relativePath.isEmpty()) {
            currentDir.delete();
        }
        return deletedCount;
    }

    private void publishCleanupAudit(int count) {
        try {
            auditTrailUtils.publishAuditTrail(
                    "SYSTEM",
                    "Media Storage",
                    AuditTrailUtils.ENTITY_ENTITY_MEDIA,
                    AuditTrailUtils.EVENT_CLEANUP,
                    "Automated cleanup deleted " + count + " unused files from " + sharedFilePath,
                    0L // System ID
            );
        } catch (Exception e) {
            log.error("Failed to publish cleanup audit: {}", e.getMessage());
        }
    }
}