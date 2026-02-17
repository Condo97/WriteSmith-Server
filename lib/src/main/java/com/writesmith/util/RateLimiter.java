package com.writesmith.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Simple per-key rate limiter using a sliding window approach.
 * Thread-safe and suitable for per-user rate limiting on endpoints.
 *
 * Usage:
 *   RateLimiter limiter = new RateLimiter(10, 60_000); // 10 requests per minute
 *   if (!limiter.tryAcquire(userId)) {
 *       throw new RateLimitExceededException();
 *   }
 */
public class RateLimiter {

    private final int maxRequests;
    private final long windowMs;
    private final ConcurrentHashMap<Object, WindowEntry> windows = new ConcurrentHashMap<>();

    // Periodic cleanup of stale entries
    private static final ScheduledExecutorService cleanupScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RateLimiter-Cleanup");
                t.setDaemon(true);
                return t;
            });

    /**
     * Creates a rate limiter.
     *
     * @param maxRequests Maximum number of requests allowed per window
     * @param windowMs   Time window in milliseconds
     */
    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;

        // Schedule periodic cleanup every 5 minutes
        cleanupScheduler.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Attempts to acquire a permit for the given key.
     *
     * @param key The key to rate limit on (e.g., user ID)
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public boolean tryAcquire(Object key) {
        long now = System.currentTimeMillis();
        WindowEntry entry = windows.computeIfAbsent(key, k -> new WindowEntry());

        synchronized (entry) {
            // Remove timestamps outside the window
            while (!entry.timestamps.isEmpty() && entry.timestamps.peekFirst() <= now - windowMs) {
                entry.timestamps.pollFirst();
            }

            if (entry.timestamps.size() < maxRequests) {
                entry.timestamps.addLast(now);
                return true;
            }

            return false;
        }
    }

    /**
     * Returns the number of remaining requests for the given key in the current window.
     */
    public int remaining(Object key) {
        long now = System.currentTimeMillis();
        WindowEntry entry = windows.get(key);
        if (entry == null) return maxRequests;

        synchronized (entry) {
            while (!entry.timestamps.isEmpty() && entry.timestamps.peekFirst() <= now - windowMs) {
                entry.timestamps.pollFirst();
            }
            return Math.max(0, maxRequests - entry.timestamps.size());
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Object, WindowEntry>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, WindowEntry> entry = it.next();
            WindowEntry window = entry.getValue();
            synchronized (window) {
                while (!window.timestamps.isEmpty() && window.timestamps.peekFirst() <= now - windowMs) {
                    window.timestamps.pollFirst();
                }
                if (window.timestamps.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private static class WindowEntry {
        final ConcurrentLinkedDeque<Long> timestamps = new ConcurrentLinkedDeque<>();
    }

}
