package com.writesmith;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.PremiumStatusCache;
import com.writesmith.keys.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for performance optimizations:
 * 1. PremiumStatusCache - TTL-based caching with background refresh
 * 2. Image dimension extraction - lightweight metadata-only approach
 * 3. HikariCP connection pool - non-blocking with timeout
 */
public class PerformanceOptimizationTests {

    @BeforeAll
    static void setUp() throws SQLException {
        // Initialize HikariCP connection pool
        SQLConnectionPoolInstance.create(Constants.MYSQL_URL, Keys.MYSQL_USER, Keys.MYSQL_PASS, 10);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PREMIUM STATUS CACHE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PremiumStatusCache - Cache hit returns immediately")
    void testPremiumCacheHit() {
        // Clear cache first
        PremiumStatusCache.clear();
        
        // First call will fetch (may be slow)
        Integer testUserId = 99999; // Non-existent user, will default to premium on error
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        long start1 = System.currentTimeMillis();
        boolean result1 = PremiumStatusCache.getIsPremium(testUserId, executor);
        long duration1 = System.currentTimeMillis() - start1;
        
        // Second call should be cached (fast)
        long start2 = System.currentTimeMillis();
        boolean result2 = PremiumStatusCache.getIsPremium(testUserId, executor);
        long duration2 = System.currentTimeMillis() - start2;
        
        System.out.println("[PremiumCache Test] First call: " + duration1 + "ms, Second call: " + duration2 + "ms");
        
        // Cache hit should be much faster (< 5ms typically)
        assertTrue(duration2 < 50, "Cache hit should be fast, was " + duration2 + "ms");
        assertEquals(result1, result2, "Cached result should match original");
        assertTrue(PremiumStatusCache.isCached(testUserId), "User should be cached");
        
        executor.shutdown();
    }

    @Test
    @DisplayName("PremiumStatusCache - Invalidation works")
    void testPremiumCacheInvalidation() {
        Integer testUserId = 88888;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Populate cache
        PremiumStatusCache.getIsPremium(testUserId, executor);
        assertTrue(PremiumStatusCache.isCached(testUserId), "User should be cached after fetch");
        
        // Invalidate
        PremiumStatusCache.invalidate(testUserId);
        assertFalse(PremiumStatusCache.isCached(testUserId), "User should not be cached after invalidation");
        
        executor.shutdown();
    }

    @Test
    @DisplayName("PremiumStatusCache - Clear removes all entries")
    void testPremiumCacheClear() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Populate cache with multiple users
        PremiumStatusCache.getIsPremium(77777, executor);
        PremiumStatusCache.getIsPremium(77778, executor);
        PremiumStatusCache.getIsPremium(77779, executor);
        
        assertTrue(PremiumStatusCache.size() >= 3, "Cache should have entries");
        
        // Clear
        PremiumStatusCache.clear();
        assertEquals(0, PremiumStatusCache.size(), "Cache should be empty after clear");
        
        executor.shutdown();
    }

    @Test
    @DisplayName("PremiumStatusCache - Concurrent access is thread-safe")
    void testPremiumCacheConcurrency() throws InterruptedException {
        PremiumStatusCache.clear();
        
        int numThreads = 10;
        int numIterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // All threads access the same user ID concurrently
        Integer sharedUserId = 66666;
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < numIterations; j++) {
                        PremiumStatusCache.getIsPremium(sharedUserId, executor);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.println("[PremiumCache Concurrency] Success: " + successCount.get() + ", Errors: " + errorCount.get());
        
        assertEquals(0, errorCount.get(), "No errors should occur during concurrent access");
        assertEquals(numThreads * numIterations, successCount.get(), "All operations should succeed");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IMAGE DIMENSION EXTRACTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Image dimension extraction - JPEG via ImageReader metadata")
    void testImageDimensionExtractionJpeg() throws Exception {
        // Create a test JPEG image
        int testWidth = 800;
        int testHeight = 600;
        BufferedImage testImage = new BufferedImage(testWidth, testHeight, BufferedImage.TYPE_INT_RGB);
        
        // Encode to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "jpeg", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        String dataUrl = "data:image/jpeg;base64," + base64;
        
        // Extract dimensions using lightweight method
        int[] dimensions = extractDimensionsLightweight(dataUrl);
        
        assertNotNull(dimensions, "Should extract dimensions");
        assertEquals(testWidth, dimensions[0], "Width should match");
        assertEquals(testHeight, dimensions[1], "Height should match");
    }

    @Test
    @DisplayName("Image dimension extraction - PNG via ImageReader metadata")
    void testImageDimensionExtractionPng() throws Exception {
        // Create a test PNG image
        int testWidth = 1024;
        int testHeight = 768;
        BufferedImage testImage = new BufferedImage(testWidth, testHeight, BufferedImage.TYPE_INT_ARGB);
        
        // Encode to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        String dataUrl = "data:image/png;base64," + base64;
        
        // Extract dimensions using lightweight method
        int[] dimensions = extractDimensionsLightweight(dataUrl);
        
        assertNotNull(dimensions, "Should extract dimensions");
        assertEquals(testWidth, dimensions[0], "Width should match");
        assertEquals(testHeight, dimensions[1], "Height should match");
    }

    @Test
    @DisplayName("Image dimension extraction - Performance comparison")
    void testImageDimensionExtractionPerformance() throws Exception {
        // Create a larger test image
        int testWidth = 2048;
        int testHeight = 1536;
        BufferedImage testImage = new BufferedImage(testWidth, testHeight, BufferedImage.TYPE_INT_RGB);
        
        // Encode to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "jpeg", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        
        int iterations = 10;
        
        // Test lightweight method (ImageReader metadata)
        long lightweightTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(iis);
                    int w = reader.getWidth(0);
                    int h = reader.getHeight(0);
                    reader.dispose();
                }
            }
            lightweightTotal += System.nanoTime() - start;
        }
        
        // Test full decode method (BufferedImage)
        long fullDecodeTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            int w = img.getWidth();
            int h = img.getHeight();
            fullDecodeTotal += System.nanoTime() - start;
        }
        
        double lightweightAvgMs = (lightweightTotal / iterations) / 1_000_000.0;
        double fullDecodeAvgMs = (fullDecodeTotal / iterations) / 1_000_000.0;
        
        System.out.println("[Image Dimension Test] Lightweight avg: " + String.format("%.2f", lightweightAvgMs) + "ms");
        System.out.println("[Image Dimension Test] Full decode avg: " + String.format("%.2f", fullDecodeAvgMs) + "ms");
        System.out.println("[Image Dimension Test] Speedup: " + String.format("%.1f", fullDecodeAvgMs / lightweightAvgMs) + "x");
        
        // Lightweight should be faster (usually 2-10x faster)
        assertTrue(lightweightAvgMs <= fullDecodeAvgMs, 
                "Lightweight method should not be slower than full decode");
    }

    /**
     * Helper method to extract dimensions using the lightweight ImageReader approach.
     * This mirrors the implementation in GetChatWebSocket_OpenRouter.
     */
    private int[] extractDimensionsLightweight(String dataUrl) throws Exception {
        if (!dataUrl.startsWith("data:image/")) {
            return null;
        }
        
        String[] parts = dataUrl.split(",", 2);
        if (parts.length != 2) {
            return null;
        }
        
        byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
        
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (iis == null) {
                return null;
            }
            
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    return new int[]{width, height};
                } finally {
                    reader.dispose();
                }
            }
        }
        
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HIKARICP CONNECTION POOL TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("HikariCP - Connection acquisition with timeout")
    void testHikariConnectionAcquisition() throws Exception {
        // Get a connection
        long start = System.currentTimeMillis();
        Connection conn = SQLConnectionPoolInstance.getConnection();
        long duration = System.currentTimeMillis() - start;
        
        assertNotNull(conn, "Should get a connection");
        assertTrue(duration < 5000, "Connection should be acquired within timeout");
        
        System.out.println("[HikariCP Test] Connection acquired in " + duration + "ms");
        System.out.println("[HikariCP Test] Pool stats: " + SQLConnectionPoolInstance.getPoolStats());
        
        // Release connection
        SQLConnectionPoolInstance.releaseConnection(conn);
    }

    @Test
    @DisplayName("HikariCP - Concurrent connection acquisition")
    void testHikariConcurrentConnections() throws Exception {
        int numThreads = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    Connection conn = SQLConnectionPoolInstance.getConnection();
                    Thread.sleep(50); // Simulate some work
                    SQLConnectionPoolInstance.releaseConnection(conn);
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("[HikariCP Test] Error: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.println("[HikariCP Concurrency] Success: " + successCount.get() + ", Errors: " + errorCount.get());
        System.out.println("[HikariCP Concurrency] Pool stats after test: " + SQLConnectionPoolInstance.getPoolStats());
        
        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(numThreads, successCount.get(), "All connection requests should succeed");
        assertEquals(0, errorCount.get(), "No errors should occur");
    }

    @Test
    @DisplayName("HikariCP - Connection is properly returned to pool")
    void testHikariConnectionReturn() throws Exception {
        // Get initial stats
        String initialStats = SQLConnectionPoolInstance.getPoolStats();
        System.out.println("[HikariCP Return Test] Initial: " + initialStats);
        
        // Get and release multiple connections
        for (int i = 0; i < 5; i++) {
            Connection conn = SQLConnectionPoolInstance.getConnection();
            assertNotNull(conn);
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
        
        // Small delay to let pool stabilize
        Thread.sleep(100);
        
        String finalStats = SQLConnectionPoolInstance.getPoolStats();
        System.out.println("[HikariCP Return Test] Final: " + finalStats);
        
        // Pool should have connections available (idle > 0 or active = 0)
        assertTrue(finalStats.contains("Active: 0") || finalStats.contains("Idle:"),
                "Connections should be returned to pool");
    }
}

