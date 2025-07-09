package pl.planzy.place.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.planzy.place.exception.PlaceServiceException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for caching place data to improve performance.
 * <p>
 * This service maintains an in-memory cache of Place IDs indexed by
 * different lookup keys to reduce the need for database queries and external API calls.
 * The cache helps improve application performance by:
 * <ul>
 *   <li>Reducing database load for repeated place lookups</li>
 *   <li>Minimizing external API calls to Google Places</li>
 *   <li>Supporting high-concurrency scenarios with thread-safe operations</li>
 * </ul>
 * <p>
 * The cache is implemented using {@link ConcurrentHashMap} to ensure thread safety
 * without requiring explicit synchronization for most operations.
 * <p>
 * This service includes comprehensive logging and exception handling to help
 * diagnose cache-related issues in production environments.
 */
@Service
public class PlaceCacheService {
    private static final Logger logger = LoggerFactory.getLogger(PlaceCacheService.class);

    /**
     * Thread-safe map to store Google Place IDs by location and name.
     * This helps avoid redundant API calls for already resolved places.
     * Key format: "placeName|location"
     */
    private final Map<String, String> placeIdCache = new ConcurrentHashMap<>();

    /**
     * Retrieves a Google Place ID from the cache.
     * <p>
     * This method performs a cache lookup based on the place name and location
     * and returns the associated Google Place ID if found. A null return value
     * indicates a cache miss.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param placeName The name of the place
     * @param location The location of the place
     * @return The cached Google Place ID, or null if not found
     * @throws PlaceServiceException if there's an unexpected error during cache access
     */
    public String getPlaceIdByLocation(String placeName, String location) {
        if (placeName == null) {
            logger.warn("Attempted to get Google Place ID with null place name");
            return null;
        }

        try {
            // Combine name and location as the cache key
            String cacheKey = createCacheKey(placeName, location);
            logger.debug("Cache lookup for place: {} at location: {}", placeName, location);

            String googlePlaceId = placeIdCache.get(cacheKey);

            if (googlePlaceId != null) {
                logger.debug("Cache hit for place: {} at location: {}", placeName, location);
            } else {
                logger.debug("Cache miss for place: {} at location: {}", placeName, location);
            }

            return googlePlaceId;
        } catch (Exception e) {
            logger.error("Error retrieving Google Place ID from cache for place: {} at location: {}",
                    placeName, location, e);
            throw new PlaceServiceException("Failed to retrieve Google Place ID from cache", e);
        }
    }

    /**
     * Adds or updates a Google Place ID in the cache.
     * <p>
     * This method associates a place name and location with its Google Place ID,
     * allowing future lookups to bypass Google Places API calls. If the place already
     * exists in the cache, its ID is updated with the new value.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param placeName The name of the place
     * @param location The location of the place
     * @param googlePlaceId The Google Place ID to cache
     * @throws PlaceServiceException if there's an unexpected error during cache update
     */
    public void cachePlaceIdByLocation(String placeName, String location, String googlePlaceId) {
        if (placeName == null) {
            logger.warn("Attempted to cache Google Place ID with null place name");
            return;
        }

        if (googlePlaceId == null) {
            logger.warn("Attempted to cache null Google Place ID for place: {} at location: {}",
                    placeName, location);
            return;
        }

        try {
            String cacheKey = createCacheKey(placeName, location);
            logger.debug("Caching Google Place ID: {} for place: {} at location: {}",
                    googlePlaceId, placeName, location);
            placeIdCache.put(cacheKey, googlePlaceId);
        } catch (Exception e) {
            logger.error("Error caching Google Place ID for place: {} at location: {}",
                    placeName, location, e);
            throw new PlaceServiceException("Failed to cache Google Place ID", e);
        }
    }

    /**
     * Creates a consistent cache key from place name and location.
     *
     * @param placeName The name of the place
     * @param location The location of the place
     * @return A combined key for cache lookups
     */
    private String createCacheKey(String placeName, String location) {
        return placeName + "|" + (location != null ? location : "");
    }

    /**
     * Clears all cached place data.
     * <p>
     * This method removes all entries from the place cache, forcing future
     * lookups to query the database or external API. It should be called when:
     * <ul>
     *   <li>Underlying data might have changed from external sources</li>
     *   <li>Cache inconsistencies are detected</li>
     *   <li>Memory pressure requires freeing up resources</li>
     * </ul>
     * <p>
     * The method includes logging and exception handling to track cache
     * clearing operations and issues.
     *
     * @throws PlaceServiceException if there's an unexpected error during cache clearing
     */
    public void clearCache() {
        try {
            logger.info("Clearing place cache. Current size: {}", placeIdCache.size());
            placeIdCache.clear();
            logger.info("Place cache cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing place cache", e);
            throw new PlaceServiceException("Failed to clear place cache", e);
        }
    }

    /**
     * Returns the number of places currently stored in the cache.
     * <p>
     * This method provides a diagnostic view of the cache size, which can be
     * useful for monitoring cache utilization and detecting memory leaks or
     * unexpected growth patterns.
     *
     * @return The number of places in the cache
     * @throws PlaceServiceException if there's an unexpected error accessing the cache
     */
    public int getCachedPlaceCount() {
        try {
            int size = placeIdCache.size();
            logger.debug("Retrieved cache size: {}", size);
            return size;
        } catch (Exception e) {
            logger.error("Error getting cache size", e);
            throw new PlaceServiceException("Failed to get place cache size", e);
        }
    }
}