package pl.planzy.artist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.planzy.artist.exception.ArtistServiceException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for caching artist data to improve performance.
 * <p>
 * This service maintains an in-memory cache of artist IDs indexed by their names,
 * reducing the need for database lookups for frequently accessed artists.
 * The cache helps improve application performance by:
 * <ul>
 *   <li>Reducing database load for repeated artist lookups</li>
 *   <li>Minimizing latency for artist name to ID resolution</li>
 *   <li>Supporting high-concurrency scenarios with thread-safe operations</li>
 * </ul>
 * <p>
 * The cache is implemented using a {@link ConcurrentHashMap} to ensure thread safety
 * without requiring explicit synchronization for most operations.
 * <p>
 * This service includes comprehensive logging and exception handling to help
 * diagnose cache-related issues in production environments.
 */

@Service
public class ArtistCacheService {
    private static final Logger logger = LoggerFactory.getLogger(ArtistCacheService.class);

    /**
     * Thread-safe map to store artist IDs by name.
     * Uses ConcurrentHashMap to avoid race conditions in concurrent access scenarios.
     */
    private final Map<String, Long> artistIdCache = new ConcurrentHashMap<>();

    /**
     * Retrieves an artist ID from the cache.
     * <p>
     * This method performs a cache lookup based on the artist name and returns
     * the associated ID if found. A null return value indicates a cache miss.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param name The name of the artist to lookup
     * @return The cached artist ID, or null if not found
     * @throws ArtistServiceException if there's an unexpected error during cache access
     */
    public Long getArtistIdByName(String name) {
        if (name == null) {
            logger.warn("Attempted to get artist ID with null name");
            return null;
        }

        try {
            logger.debug("Cache lookup for artist name: {}", name);
            Long artistId = artistIdCache.get(name);

            if (artistId != null) {
                logger.debug("Cache hit for artist name: {}", name);
            } else {
                logger.debug("Cache miss for artist name: {}", name);
            }

            return artistId;
        } catch (Exception e) {
            logger.error("Error retrieving artist ID from cache for name: {}", name, e);
            throw new ArtistServiceException("Failed to retrieve artist ID from cache", e);
        }
    }

    /**
     * Adds or updates an artist ID in the cache.
     * <p>
     * This method associates an artist name with its database ID in the cache,
     * allowing future lookups to bypass database queries. If the artist name
     * already exists in the cache, its ID is updated with the new value.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param name The name of the artist
     * @param id The ID of the artist to cache
     * @throws ArtistServiceException if there's an unexpected error during cache update
     */
    public void cacheArtistIdByName(String name, Long id) {
        if (name == null) {
            logger.warn("Attempted to cache artist ID with null name");
            return;
        }

        if (id == null) {
            logger.warn("Attempted to cache null ID for artist name: {}", name);
            return;
        }

        try {
            logger.debug("Caching artist ID: {} for name: {}", id, name);
            artistIdCache.put(name, id);
        } catch (Exception e) {
            logger.error("Error caching artist ID for name: {}", name, e);
            throw new ArtistServiceException("Failed to cache artist ID", e);
        }
    }

    /**
     * Clears all cached artist data.
     * <p>
     * This method removes all entries from the artist cache, forcing future
     * lookups to query the database. It should be called when:
     * <ul>
     *   <li>Underlying data might have changed from external sources</li>
     *   <li>Cache inconsistencies are detected</li>
     *   <li>Memory pressure requires freeing up resources</li>
     * </ul>
     * <p>
     * The method includes logging and exception handling to track cache
     * clearing operations and issues.
     *
     * @throws ArtistServiceException if there's an unexpected error during cache clearing
     */

    public void clearCache() {
        try {
            logger.info("Clearing artist cache. Current size: {}", artistIdCache.size());
            artistIdCache.clear();
            logger.info("Artist cache cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing artist cache", e);
            throw new ArtistServiceException("Failed to clear artist cache", e);
        }
    }

    /**
     * Returns the number of artists currently stored in the cache.
     * <p>
     * This method provides a diagnostic view of the cache size, which can be
     * useful for monitoring cache utilization and detecting memory leaks or
     * unexpected growth patterns.
     *
     * @return The number of artists in the cache
     * @throws ArtistServiceException if there's an unexpected error accessing the cache
     */
    public int getCachedArtistCount() {
        try {
            int size = artistIdCache.size();
            logger.debug("Retrieved cache size: {}", size);
            return size;
        } catch (Exception e) {
            logger.error("Error getting cache size", e);
            throw new ArtistServiceException("Failed to get artist cache size", e);
        }
    }
}