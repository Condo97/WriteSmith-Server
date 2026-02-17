package com.writesmith.core;

import com.writesmith.util.PersistentLogger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory cache for premium status to avoid blocking Apple API calls on every request.
 * 
 * Features:
 * - TTL-based expiration (1 minute default)
 * - Background refresh when cache is getting stale (at 75% of TTL)
 * - Thread-safe with ConcurrentHashMap
 * - Optimistic return: if cache exists but is stale, returns cached value while refreshing
 * - Periodic eviction of expired entries to prevent memory leaks
 * - Maximum cache size to bound memory usage
 * 
 * This dramatically improves WebSocket throughput by avoiding synchronous Apple API calls
 * for every request with images.
 */
public class PremiumStatusCache {

    private static final ConcurrentHashMap<Integer, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // Cache configuration
    private static final long CACHE_TTL_MS = 60_000; // 1 minute cache lifetime
    private static final long REFRESH_THRESHOLD_MS = 45_000; // Start background refresh at 75% of TTL
    private static final int MAX_CACHE_SIZE = 10_000; // Maximum entries to prevent unbounded growth
    private static final long EVICTION_INTERVAL_MS = 120_000; // Run eviction every 2 minutes

    // Scheduled eviction
    private static final ScheduledExecutorService evictionScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PremiumCache-Eviction");
                t.setDaemon(true);
                return t;
            });

    static {
        // Start periodic eviction of expired entries
        evictionScheduler.scheduleAtFixedRate(
                PremiumStatusCache::evictExpiredEntries,
                EVICTION_INTERVAL_MS,
                EVICTION_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cache entry holding premium status and timing information.
     */
    private static class CacheEntry {
        final boolean isPremium;
        final long createdAt;
        final AtomicBoolean isRefreshing;
        
        CacheEntry(boolean isPremium) {
            this.isPremium = isPremium;
            this.createdAt = System.currentTimeMillis();
            this.isRefreshing = new AtomicBoolean(false);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
        
        boolean shouldRefresh() {
            return System.currentTimeMillis() - createdAt > REFRESH_THRESHOLD_MS;
        }
        
        boolean startRefreshing() {
            return isRefreshing.compareAndSet(false, true);
        }

        long age() {
            return System.currentTimeMillis() - createdAt;
        }
    }
    
    /**
     * Gets the premium status for a user, using cache when available.
     * 
     * Behavior:
     * 1. If cached and fresh: return immediately
     * 2. If cached but stale (not expired): return cached, trigger background refresh
     * 3. If cached but expired: fetch synchronously, update cache
     * 4. If not cached: fetch synchronously, update cache
     * 
     * @param userId The user ID to check premium status for
     * @param executor ExecutorService to use for background refresh (can be null to skip background refresh)
     * @return true if user is premium, false otherwise
     */
    public static boolean getIsPremium(Integer userId, ExecutorService executor) {
        CacheEntry entry = cache.get(userId);
        
        if (entry != null) {
            // Cache hit - check if we need to refresh in background
            if (entry.shouldRefresh() && !entry.isExpired() && executor != null) {
                // Try to start background refresh (only one thread will succeed)
                if (entry.startRefreshing()) {
                    refreshAsync(userId, executor);
                }
            }
            
            // Return cached value if not expired
            if (!entry.isExpired()) {
                return entry.isPremium;
            }
        }
        
        // No cache or expired: fetch synchronously
        return fetchAndCache(userId);
    }
    
    /**
     * Fetches premium status synchronously and caches the result.
     * Used for first-time lookups or when cache has fully expired.
     *
     * Fail-open policy:
     * - If a stale cached value exists, use it (protect paying users)
     * - If no cached value exists (first-time check), default to true (fail-open)
     *   to protect paying users during Apple outages. The risk of a free user
     *   getting temporary premium is far lower than a paying user losing access.
     */
    private static boolean fetchAndCache(Integer userId) {
        try {
            boolean isPremium = WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(userId);
            putWithSizeCheck(userId, new CacheEntry(isPremium));
            PersistentLogger.info(PersistentLogger.APPLE, "[PremiumCache] Cached premium status for user " + userId + ": " + isPremium);
            return isPremium;
        } catch (Exception e) {
            // On error, check if we have a stale cached value we can use
            CacheEntry staleEntry = cache.get(userId);
            if (staleEntry != null) {
                PersistentLogger.warn(PersistentLogger.APPLE, "[PremiumCache] Error fetching premium for user " + userId + 
                                 ", using stale cached value: " + staleEntry.isPremium + 
                                 " (error: " + e.getMessage() + ")");
                return staleEntry.isPremium;
            }
            
            // No cached value and error - fail-open: default to premium
            // The risk of a free user getting temporary premium during an Apple outage
            // is far lower than the risk of a paying user losing access.
            PersistentLogger.warn(PersistentLogger.APPLE, "[PremiumCache] Error fetching premium for user " + userId + 
                             ", no cached value available, defaulting to PREMIUM to protect paying users (error: " + e.getMessage() + ")");
            return true;
        }
    }
    
    /**
     * Refreshes premium status in the background without blocking the caller.
     */
    private static void refreshAsync(Integer userId, ExecutorService executor) {
        executor.submit(() -> {
            try {
                boolean isPremium = WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(userId);
                putWithSizeCheck(userId, new CacheEntry(isPremium));
                PersistentLogger.info(PersistentLogger.APPLE, "[PremiumCache] Background refresh completed for user " + userId + ": " + isPremium);
            } catch (Exception e) {
                // Background refresh failed - log but don't worry, cached value still valid
                PersistentLogger.warn(PersistentLogger.APPLE, "[PremiumCache] Background refresh failed for user " + userId + 
                                 ": " + e.getMessage());
                // Reset the refreshing flag so next request can try again
                CacheEntry entry = cache.get(userId);
                if (entry != null) {
                    entry.isRefreshing.set(false);
                }
            }
        });
    }

    /**
     * Puts an entry in the cache with size checking.
     * If cache exceeds MAX_CACHE_SIZE, evicts expired entries first,
     * then oldest entries if still over limit.
     */
    private static void putWithSizeCheck(Integer userId, CacheEntry entry) {
        cache.put(userId, entry);

        if (cache.size() > MAX_CACHE_SIZE) {
            evictExpiredEntries();

            // If still over limit after evicting expired, remove oldest entries
            if (cache.size() > MAX_CACHE_SIZE) {
                int toRemove = cache.size() - MAX_CACHE_SIZE + (MAX_CACHE_SIZE / 10); // Remove 10% extra
                cache.entrySet().stream()
                        .sorted((a, b) -> Long.compare(a.getValue().createdAt, b.getValue().createdAt))
                        .limit(toRemove)
                        .forEach(e -> cache.remove(e.getKey()));
                PersistentLogger.info(PersistentLogger.APPLE, "[PremiumCache] Evicted " + toRemove + " oldest entries (cache size exceeded " + MAX_CACHE_SIZE + ")");
            }
        }
    }

    /**
     * Removes all expired entries from the cache.
     * Called periodically by the eviction scheduler and on size overflow.
     */
    private static void evictExpiredEntries() {
        int evicted = 0;
        Iterator<Map.Entry<Integer, CacheEntry>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, CacheEntry> entry = it.next();
            if (entry.getValue().isExpired()) {
                it.remove();
                evicted++;
            }
        }
        if (evicted > 0) {
            PersistentLogger.debug(PersistentLogger.APPLE, "[PremiumCache] Evicted " + evicted + " expired entries. Cache size: " + cache.size());
        }
    }

    /**
     * Pre-warms the cache for a user. Useful to call after successful auth
     * to have premium status ready before it's needed.
     * 
     * @param userId The user ID to pre-warm cache for
     * @param executor ExecutorService to use for background fetch
     */
    public static void preWarm(Integer userId, ExecutorService executor) {
        if (executor != null && !cache.containsKey(userId)) {
            executor.submit(() -> fetchAndCache(userId));
        }
    }
    
    /**
     * Invalidates the cache for a specific user.
     * Call this when a user's subscription status changes (e.g., after purchase).
     */
    public static void invalidate(Integer userId) {
        cache.remove(userId);
        PersistentLogger.info(PersistentLogger.APPLE, "[PremiumCache] Invalidated cache for user " + userId);
    }
    
    /**
     * Clears the entire cache.
     * Useful for testing or when subscription system changes globally.
     */
    public static void clear() {
        cache.clear();
        PersistentLogger.info(PersistentLogger.APPLE, "[PremiumCache] Cache cleared");
    }
    
    /**
     * Returns the current cache size (for monitoring).
     */
    public static int size() {
        return cache.size();
    }
    
    /**
     * Checks if a user has a cached premium status (for debugging).
     */
    public static boolean isCached(Integer userId) {
        CacheEntry entry = cache.get(userId);
        return entry != null && !entry.isExpired();
    }
}
