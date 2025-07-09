package pl.planzy.tag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.planzy.tag.exception.TagServiceException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for caching tag data to improve performance.
 * <p>
 * This service maintains an in-memory cache of tag IDs indexed by their names,
 * reducing the need for database lookups for frequently accessed tags.
 * The cache helps improve application performance by:
 * <ul>
 *   <li>Reducing database load for repeated tag lookups</li>
 *   <li>Minimizing latency for tag name to ID resolution</li>
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
public class TagCacheService {
    private static final Logger logger = LoggerFactory.getLogger(TagCacheService.class);

    /**
     * Thread-safe map to store tag IDs by name.
     * Uses ConcurrentHashMap to avoid race conditions in concurrent access scenarios.
     */
    private final Map<String, Long> tagIdCache = new ConcurrentHashMap<>();

    /**
     * Retrieves a tag ID from the cache.
     * <p>
     * This method performs a cache lookup based on the tag name and returns
     * the associated ID if found. A null return value indicates a cache miss.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param name The name of the tag to lookup
     * @return The cached tag ID, or null if not found
     * @throws TagServiceException if there's an unexpected error during cache access
     */
    public Long getTagIdByName(String name) {
        if (name == null) {
            logger.warn("Attempted to get tag ID with null name");
            return null;
        }

        try {
            logger.debug("Cache lookup for tag name: {}", name);
            Long tagId = tagIdCache.get(name);

            if (tagId != null) {
                logger.debug("Cache hit for tag name: {}", name);
            } else {
                logger.debug("Cache miss for tag name: {}", name);
            }

            return tagId;
        } catch (Exception e) {
            logger.error("Error retrieving tag ID from cache for name: {}", name, e);
            throw new TagServiceException("Failed to retrieve tag ID from cache", e);
        }
    }

    /**
     * Adds or updates a tag ID in the cache.
     * <p>
     * This method associates a tag name with its database ID in the cache,
     * allowing future lookups to bypass database queries. If the tag name
     * already exists in the cache, its ID is updated with the new value.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param name The name of the tag
     * @param id The ID of the tag to cache
     * @throws TagServiceException if there's an unexpected error during cache update
     */
    public void cacheTagIdByName(String name, Long id) {
        if (name == null) {
            logger.warn("Attempted to cache tag ID with null name");
            return;
        }

        if (id == null) {
            logger.warn("Attempted to cache null ID for tag name: {}", name);
            return;
        }

        try {
            logger.debug("Caching tag ID: {} for name: {}", id, name);
            tagIdCache.put(name, id);
        } catch (Exception e) {
            logger.error("Error caching tag ID for name: {}", name, e);
            throw new TagServiceException("Failed to cache tag ID", e);
        }
    }

    /**
     * Clears all cached tag data.
     * <p>
     * This method removes all entries from the tag cache, forcing future
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
     * @throws TagServiceException if there's an unexpected error during cache clearing
     */
    public void clearCache() {
        try {
            logger.info("Clearing tag cache. Current size: {}", tagIdCache.size());
            tagIdCache.clear();
            logger.info("Tag cache cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing tag cache", e);
            throw new TagServiceException("Failed to clear tag cache", e);
        }
    }

    /**
     * Returns the number of tags currently stored in the cache.
     * <p>
     * This method provides a diagnostic view of the cache size, which can be
     * useful for monitoring cache utilization and detecting memory leaks or
     * unexpected growth patterns.
     *
     * @return The number of tags in the cache
     * @throws TagServiceException if there's an unexpected error accessing the cache
     */
    public int getCachedTagCount() {
        try {
            int size = tagIdCache.size();
            logger.debug("Retrieved cache size: {}", size);
            return size;
        } catch (Exception e) {
            logger.error("Error getting cache size", e);
            throw new TagServiceException("Failed to get tag cache size", e);
        }
    }
}