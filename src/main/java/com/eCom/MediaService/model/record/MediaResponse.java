package com.eCom.MediaService.model.record;

import com.fasterxml.jackson.annotation.JsonInclude;

// ✅ JsonInclude ensures we don't send null fields to the frontend
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaResponse(
        boolean success,
        String tempUrl,
        String message
) {
    // Static factory methods for cleaner code
    public static MediaResponse ok(String url) {
        return new MediaResponse(true, url, null);
    }

    public static MediaResponse error(String msg) {
        return new MediaResponse(false, null, msg);
    }
}
