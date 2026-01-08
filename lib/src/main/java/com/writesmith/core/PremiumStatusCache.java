package com.writesmith.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory cache for premium status to avoid blocking Apple API calls on every request.
 * 
 * Features:
 * - TTL-based expiration (1 minute default)
 * - Background refresh when cache is getting stale (at 75% of TTL)
 * - Thread-safe with ConcurrentHashMap
 * - Optimistic return: if cache exists but is stale, returns cached value while refreshing
 * 
 * This dramatically improves WebSocket throughput by avoiding synchronous Apple API calls
 * for every request with images.
 */
public class PremiumStatusCache {

    private static final ConcurrentHashMap<Integer, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // Cache configuration
    private static final long CACHE_TTL_MS = 60_000; // 1 minute cache lifetime
    private static final long REFRESH_THRESHOLD_MS = 45_000; // Start background refresh at 75% of TTL
    
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
     */
    private static boolean fetchAndCache(Integer userId) {
        try {
            boolean isPremium = WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(userId);
            cache.put(userId, new CacheEntry(isPremium));
            System.out.println("[PremiumCache] Cached premium status for user " + userId + ": " + isPremium);
            return isPremium;
        } catch (Exception e) {
            // On error, check if we have a stale cached value we can use
            CacheEntry staleEntry = cache.get(userId);
            if (staleEntry != null) {
                System.out.println("[PremiumCache] Error fetching premium for user " + userId + 
                                 ", using stale cached value: " + staleEntry.isPremium + 
                                 " (error: " + e.getMessage() + ")");
                return staleEntry.isPremium;
            }
            
            // No cached value and error - default to premium (fail-open to not block paying users)
            System.out.println("[PremiumCache] Error fetching premium for user " + userId + 
                             ", defaulting to premium (error: " + e.getMessage() + ")");
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
                cache.put(userId, new CacheEntry(isPremium));
                System.out.println("[PremiumCache] Background refresh completed for user " + userId + ": " + isPremium);
            } catch (Exception e) {
                // Background refresh failed - log but don't worry, cached value still valid
                System.out.println("[PremiumCache] Background refresh failed for user " + userId + 
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
        System.out.println("[PremiumCache] Invalidated cache for user " + userId);
    }
    
    /**
     * Clears the entire cache.
     * Useful for testing or when subscription system changes globally.
     */
    public static void clear() {
        cache.clear();
        System.out.println("[PremiumCache] Cache cleared");
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

