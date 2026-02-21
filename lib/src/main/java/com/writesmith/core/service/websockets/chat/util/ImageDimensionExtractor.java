package com.writesmith.core.service.websockets.chat.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Iterator;

public final class ImageDimensionExtractor {

    private ImageDimensionExtractor() {}

    public static final class Result {
        private final String url;
        private final int width;
        private final int height;
        private final boolean dimensionsKnown;

        public Result(String url, int width, int height, boolean dimensionsKnown) {
            this.url = url;
            this.width = width;
            this.height = height;
            this.dimensionsKnown = dimensionsKnown;
        }

        public static Result unknownDimensions(String url) {
            return new Result(url, 0, 0, false);
        }

        public String getUrl() { return url; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean isDimensionsKnown() { return dimensionsKnown; }
    }

    public static Result extract(String originalUrl) {
        try {
            if (!originalUrl.startsWith("data:image/")) {
                return Result.unknownDimensions(originalUrl);
            }

            String[] parts = originalUrl.split(",", 2);
            if (parts.length != 2) {
                return Result.unknownDimensions(originalUrl);
            }

            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(parts[1]);
            } catch (IllegalArgumentException e) {
                return Result.unknownDimensions(originalUrl);
            }

            try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
                if (iis != null) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                    if (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        try {
                            reader.setInput(iis);
                            return new Result(originalUrl, reader.getWidth(0), reader.getHeight(0), true);
                        } finally {
                            reader.dispose();
                        }
                    }
                }
            }

            // Fallback: full decode via BufferedImage (slower but more compatible)
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                return new Result(originalUrl, image.getWidth(), image.getHeight(), true);
            }

            return Result.unknownDimensions(originalUrl);
        } catch (Exception e) {
            return Result.unknownDimensions(originalUrl);
        }
    }
}
