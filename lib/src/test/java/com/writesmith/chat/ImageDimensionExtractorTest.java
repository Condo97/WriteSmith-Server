package com.writesmith.chat;

import com.writesmith.core.service.websockets.chat.util.ImageDimensionExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageDimensionExtractorTest {

    @Test
    void externalUrlReturnsUnknownDimensions() {
        ImageDimensionExtractor.Result result = ImageDimensionExtractor.extract("https://example.com/photo.jpg");
        assertFalse(result.isDimensionsKnown());
        assertEquals("https://example.com/photo.jpg", result.getUrl());
    }

    @Test
    void malformedDataUrlReturnsUnknown() {
        ImageDimensionExtractor.Result result = ImageDimensionExtractor.extract("data:image/png;base64");
        assertFalse(result.isDimensionsKnown());
    }

    @Test
    void invalidBase64ReturnsUnknown() {
        ImageDimensionExtractor.Result result = ImageDimensionExtractor.extract("data:image/png;base64,!!!not-base64!!!");
        assertFalse(result.isDimensionsKnown());
    }

    @Test
    void urlIsNeverModified() {
        String url = "https://cdn.example.com/image.jpg?token=abc&size=large";
        ImageDimensionExtractor.Result result = ImageDimensionExtractor.extract(url);
        assertEquals(url, result.getUrl());
    }
}
