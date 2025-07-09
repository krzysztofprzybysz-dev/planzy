package pl.planzy.tag.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.planzy.tag.domain.Tag;
import pl.planzy.tag.domain.TagRepository;
import pl.planzy.tag.exception.TagNotFoundException;
import pl.planzy.tag.exception.TagServiceException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Primary service for tag-related operations.
 * <p>
 * This service provides the main entry point for tag management operations,
 * coordinating between the repository and specialized services to implement
 * the core business logic for tags. It follows a modular design pattern
 * where each specialized service handles a specific responsibility.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Retrieving and creating tags by name</li>
 *   <li>Coordinating caching operations for performance</li>
 *   <li>Managing transactions for tag operations</li>
 *   <li>Providing consolidated statistics about the tag module</li>
 * </ul>
 * <p>
 * The service includes comprehensive logging and exception handling to ensure
 * reliable operation and facilitate troubleshooting in production environments.
 */
@Service
public class TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    /**
     * Entity manager for JPA operations.
     * Used for direct entity management when needed.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Repository for basic CRUD operations on tags.
     */
    private final TagRepository tagRepository;

    /**
     * Service for tag caching operations.
     */
    private final TagCacheService cacheService;

    /**
     * Service for complex tag queries.
     */
    private final TagQueryService queryService;

    /**
     * Constructs a new TagService with the required dependencies.
     *
     * @param tagRepository Repository for tag entity operations
     * @param cacheService Service for tag caching operations
     * @param queryService Service for complex tag queries
     */
    @Autowired
    public TagService(
            TagRepository tagRepository,
            TagCacheService cacheService,
            TagQueryService queryService) {
        this.tagRepository = tagRepository;
        this.cacheService = cacheService;
        this.queryService = queryService;
    }

    /**
     * Gets or creates tags by their names.
     * <p>
     * This method implements a sophisticated lookup and creation strategy:
     * <ol>
     *   <li>First checks the cache for each tag name</li>
     *   <li>For cache misses, queries the database for existing tags</li>
     *   <li>Creates any tags that don't exist in the database</li>
     *   <li>Updates the cache with all found or created tags</li>
     * </ol>
     * <p>
     * This approach minimizes database operations while ensuring data consistency.
     * The method is optimized for batch processing of multiple tag names and
     * includes comprehensive validation, logging, and exception handling.
     *
     * @param tagNames Set of tag names to retrieve or create
     * @return Map of tag names to their corresponding entities
     * @throws TagServiceException if there's an error during processing
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Map<String, Tag> findOrCreateTagsByNames(Set<String> tagNames) {
        if (tagNames == null) {
            logger.warn("Called findOrCreateTagsByNames with null tag names set");
            return Collections.emptyMap();
        }

        if (tagNames.isEmpty()) {
            logger.debug("Called findOrCreateTagsByNames with empty tag names set");
            return Collections.emptyMap();
        }

        // Filter out null or empty names
        Set<String> validNames = tagNames.stream()
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toSet());

        if (validNames.isEmpty()) {
            logger.warn("No valid tag names to process after filtering");
            return Collections.emptyMap();
        }

        logger.info("Finding or creating {} tags", validNames.size());

        try {
            Map<String, Tag> result = new HashMap<>();
            Set<String> tagNamesToCreate = new HashSet<>();

            // First check the cache for existing tags
            for (String name : validNames) {
                Long tagId = cacheService.getTagIdByName(name);
                if (tagId != null) {
                    try {
                        // Even if ID is cached, verify tag exists in database
                        Tag tag = tagRepository.findById(tagId)
                                .orElseThrow(() -> new TagNotFoundException(tagId));
                        result.put(name, tag);
                        logger.debug("Found tag in cache: {} (ID: {})", name, tagId);
                    } catch (TagNotFoundException e) {
                        // Handle case where cached ID no longer exists in database
                        logger.warn("Cached tag ID: {} for name: {} not found in database, will recreate", tagId, name);
                        tagNamesToCreate.add(name);
                    }
                } else {
                    tagNamesToCreate.add(name);
                }
            }

            // Process tags not found in cache
            if (!tagNamesToCreate.isEmpty()) {
                logger.debug("Looking up {} tags not found in cache", tagNamesToCreate.size());

                // Check database for existing tags we don't have cached
                List<Tag> existingTags = queryService.findTagsByNames(tagNamesToCreate);

                for (Tag tag : existingTags) {
                    String tagName = tag.getTagName();
                    result.put(tagName, tag);
                    cacheService.cacheTagIdByName(tagName, tag.getId());
                    tagNamesToCreate.remove(tagName);
                    logger.debug("Found existing tag in database: {} (ID: {})", tagName, tag.getId());
                }

                // Create any remaining tags that don't exist yet
                if (!tagNamesToCreate.isEmpty()) {
                    logger.info("Creating {} new tags", tagNamesToCreate.size());
                    List<Tag> newTags = new ArrayList<>();

                    for (String name : tagNamesToCreate) {
                        Tag newTag = new Tag();
                        newTag.setTagName(name);
                        newTags.add(newTag);
                    }

                    try {
                        // Batch save all new tags for better performance
                        List<Tag> savedTags = tagRepository.saveAll(newTags);
                        entityManager.flush(); // Ensure IDs are generated

                        for (Tag tag : savedTags) {
                            String tagName = tag.getTagName();
                            result.put(tagName, tag);
                            cacheService.cacheTagIdByName(tagName, tag.getId());
                            logger.debug("Created new tag: {} (ID: {})", tagName, tag.getId());
                        }
                    } catch (DataIntegrityViolationException e) {
                        // Handle race condition where another process created the tags concurrently
                        logger.error("Constraint violation while creating tags, possible race condition", e);
                        throw new TagServiceException("Constraint violation while creating tags. Another process may have created these tags concurrently.", e);
                    }
                }
            }

            logger.info("Successfully processed {} tags (found: {}, created: {})",
                    validNames.size(), result.size(), tagNamesToCreate.size());
            return result;

        } catch (TagServiceException | TagNotFoundException e) {
            // Just re-throw if it's already our custom exception
            throw e;
        } catch (DataAccessException e) {
            logger.error("Database error while finding or creating tags", e);
            throw new TagServiceException("Database error while finding or creating tags", e);
        } catch (Exception e) {
            logger.error("Unexpected error while finding or creating tags", e);
            throw new TagServiceException("Unexpected error while finding or creating tags", e);
        }
    }

    /**
     * Gets a tag by ID.
     * <p>
     * This method retrieves a tag entity by its ID, throwing an exception
     * if the tag doesn't exist. It's useful for cases where the tag is
     * expected to exist and its absence indicates an error condition.
     * <p>
     * The method includes validation, logging, and exception handling to help
     * diagnose missing tag issues.
     *
     * @param id The tag ID
     * @return The tag entity
     * @throws TagNotFoundException if the tag doesn't exist
     * @throws TagServiceException if there's an unexpected error
     * @throws IllegalArgumentException if the ID is null
     */
    @Transactional(readOnly = true)
    public Tag getTagById(Long id) {
        if (id == null) {
            logger.warn("Attempted to get tag with null ID");
            throw new IllegalArgumentException("Tag ID cannot be null");
        }

        try {
            logger.debug("Finding tag by ID: {}", id);

            return tagRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Tag not found with ID: {}", id);
                        return new TagNotFoundException(id);
                    });
        } catch (TagNotFoundException e) {
            // Just rethrow
            throw e;
        } catch (Exception e) {
            logger.error("Error finding tag by ID: {}", id, e);
            throw new TagServiceException("Failed to get tag by ID: " + id, e);
        }
    }

    /**
     * Clears the tag cache.
     * <p>
     * This method delegates to the cache service to clear all cached tag data.
     * It should be called when underlying data might have changed from external
     * sources to ensure the cache stays consistent with the database.
     * <p>
     * The method includes logging and exception handling to track cache clearing
     * operations and issues.
     *
     * @throws TagServiceException if there's an error clearing the cache
     */
    public void clearCache() {
        logger.info("Clearing tag cache");
        try {
            cacheService.clearCache();
            logger.info("Tag cache cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing tag cache", e);
            throw new TagServiceException("Failed to clear tag cache", e);
        }
    }

    /**
     * Gets statistics about the tag service.
     * <p>
     * This method collects and returns various statistics about the tag module,
     * which can be useful for monitoring and diagnostics. It aggregates information
     * from multiple sources including the repository and cache service.
     * <p>
     * The method includes logging and exception handling to ensure reliable
     * operation when gathering statistics.
     *
     * @return Map of statistic names to their values
     * @throws TagServiceException if there's an error gathering statistics
     */
    public Map<String, Object> getStatistics() {
        try {
            logger.debug("Gathering tag service statistics");

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTags", tagRepository.count());
            stats.put("cachedTags", cacheService.getCachedTagCount());

            logger.debug("Tag statistics: {}", stats);
            return stats;
        } catch (Exception e) {
            logger.error("Error gathering tag statistics", e);
            throw new TagServiceException("Failed to get tag statistics", e);
        }
    }
}