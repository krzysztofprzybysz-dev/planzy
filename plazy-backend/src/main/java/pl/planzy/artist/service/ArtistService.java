package pl.planzy.artist.service;

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
import pl.planzy.artist.domain.Artist;
import pl.planzy.artist.domain.ArtistRepository;
import pl.planzy.artist.exception.ArtistNotFoundException;
import pl.planzy.artist.exception.ArtistServiceException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Primary service for artist-related operations.
 * <p>
 * This service provides the main entry point for artist management operations,
 * coordinating between the repository and specialized services to implement
 * the core business logic for artists. It follows a modular design pattern
 * where each specialized service handles a specific responsibility.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Retrieving and creating artists by name</li>
 *   <li>Coordinating caching operations for performance</li>
 *   <li>Managing transactions for artist operations</li>
 *   <li>Providing consolidated statistics about the artist module</li>
 * </ul>
 * <p>
 * The service includes comprehensive logging and exception handling to ensure
 * reliable operation and facilitate troubleshooting in production environments.
 */
@Service
public class ArtistService {
    private static final Logger logger = LoggerFactory.getLogger(ArtistService.class);

    /**
     * Entity manager for JPA operations.
     * Used for direct entity management when needed.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Repository for basic CRUD operations on artists.
     */
    private final ArtistRepository artistRepository;

    /**
     * Service for artist caching operations.
     */
    private final ArtistCacheService cacheService;

    /**
     * Service for complex artist queries.
     */
    private final ArtistQueryService queryService;

    /**
     * Constructs a new ArtistService with the required dependencies.
     *
     * @param artistRepository Repository for artist entity operations
     * @param cacheService Service for artist caching operations
     * @param queryService Service for complex artist queries
     */
    @Autowired
    public ArtistService(
            ArtistRepository artistRepository,
            ArtistCacheService cacheService,
            ArtistQueryService queryService) {
        this.artistRepository = artistRepository;
        this.cacheService = cacheService;
        this.queryService = queryService;
    }

    /**
     * Gets or creates artists by their names.
     * <p>
     * This method implements a sophisticated lookup and creation strategy:
     * <ol>
     *   <li>First checks the cache for each artist name</li>
     *   <li>For cache misses, queries the database for existing artists</li>
     *   <li>Creates any artists that don't exist in the database</li>
     *   <li>Updates the cache with all found or created artists</li>
     * </ol>
     * <p>
     * This approach minimizes database operations while ensuring data consistency.
     * The method is optimized for batch processing of multiple artist names and
     * includes comprehensive validation, logging, and exception handling.
     *
     * @param artistNames Set of artist names to retrieve or create
     * @return Map of artist names to their corresponding entities
     * @throws ArtistServiceException if there's an error during processing
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Map<String, Artist> findOrCreateArtistsByNames(Set<String> artistNames) {
        if (artistNames == null) {
            logger.warn("Called findOrCreateArtistsByNames with null artist names set");
            return Collections.emptyMap();
        }

        if (artistNames.isEmpty()) {
            logger.debug("Called findOrCreateArtistsByNames with empty artist names set");
            return Collections.emptyMap();
        }

        // Filter out null or empty names
        Set<String> validNames = artistNames.stream()
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toSet());

        if (validNames.isEmpty()) {
            logger.warn("No valid artist names to process after filtering");
            return Collections.emptyMap();
        }

        logger.info("Finding or creating {} artists", validNames.size());

        try {
            Map<String, Artist> result = new HashMap<>();
            Set<String> artistNamesToCreate = new HashSet<>();

            // First check the cache for existing artists
            for (String name : validNames) {
                Long artistId = cacheService.getArtistIdByName(name);
                if (artistId != null) {
                    try {
                        // Even if ID is cached, verify artist exists in database
                        Artist artist = artistRepository.findById(artistId)
                                .orElseThrow(() -> new ArtistNotFoundException(artistId));
                        result.put(name, artist);
                        logger.debug("Found artist in cache: {} (ID: {})", name, artistId);
                    } catch (ArtistNotFoundException e) {
                        // Handle case where cached ID no longer exists in database
                        logger.warn("Cached artist ID: {} for name: {} not found in database, will recreate", artistId, name);
                        artistNamesToCreate.add(name);
                    }
                } else {
                    artistNamesToCreate.add(name);
                }
            }

            // Process artists not found in cache
            if (!artistNamesToCreate.isEmpty()) {
                logger.debug("Looking up {} artists not found in cache", artistNamesToCreate.size());

                // Check database for existing artists we don't have cached
                List<Artist> existingArtists = queryService.findArtistsByNames(artistNamesToCreate);

                for (Artist artist : existingArtists) {
                    String artistName = artist.getArtistName();
                    result.put(artistName, artist);
                    cacheService.cacheArtistIdByName(artistName, artist.getId());
                    artistNamesToCreate.remove(artistName);
                    logger.debug("Found existing artist in database: {} (ID: {})", artistName, artist.getId());
                }

                // Create any remaining artists that don't exist yet
                if (!artistNamesToCreate.isEmpty()) {
                    logger.info("Creating {} new artists", artistNamesToCreate.size());
                    List<Artist> newArtists = new ArrayList<>();

                    for (String name : artistNamesToCreate) {
                        Artist newArtist = new Artist();
                        newArtist.setArtistName(name);
                        newArtists.add(newArtist);
                    }

                    try {
                        // Batch save all new artists for better performance
                        List<Artist> savedArtists = artistRepository.saveAll(newArtists);
                        entityManager.flush(); // Ensure IDs are generated

                        for (Artist artist : savedArtists) {
                            String artistName = artist.getArtistName();
                            result.put(artistName, artist);
                            cacheService.cacheArtistIdByName(artistName, artist.getId());
                            logger.debug("Created new artist: {} (ID: {})", artistName, artist.getId());
                        }
                    } catch (DataIntegrityViolationException e) {
                        // Handle race condition where another process created the artists concurrently
                        logger.error("Constraint violation while creating artists, possible race condition", e);
                        throw new ArtistServiceException("Constraint violation while creating artists. Another process may have created these artists concurrently.", e);
                    }
                }
            }

            logger.info("Successfully processed {} artists (found: {}, created: {})",
                    validNames.size(), result.size(), artistNamesToCreate.size());
            return result;

        } catch (ArtistServiceException | ArtistNotFoundException e) {
            // Just re-throw if it's already our custom exception
            throw e;
        } catch (DataAccessException e) {
            logger.error("Database error while finding or creating artists", e);
            throw new ArtistServiceException("Database error while finding or creating artists", e);
        } catch (Exception e) {
            logger.error("Unexpected error while finding or creating artists", e);
            throw new ArtistServiceException("Unexpected error while finding or creating artists", e);
        }
    }

    /**
     * Gets an artist by ID.
     * <p>
     * This method retrieves an artist entity by its ID, throwing an exception
     * if the artist doesn't exist. It's useful for cases where the artist is
     * expected to exist and its absence indicates an error condition.
     * <p>
     * The method includes validation, logging, and exception handling to help
     * diagnose missing artist issues.
     *
     * @param id The artist ID
     * @return The artist entity
     * @throws ArtistNotFoundException if the artist doesn't exist
     * @throws ArtistServiceException if there's an unexpected error
     * @throws IllegalArgumentException if the ID is null
     */
    @Transactional(readOnly = true)
    public Artist getArtistById(Long id) {
        if (id == null) {
            logger.warn("Attempted to get artist with null ID");
            throw new IllegalArgumentException("Artist ID cannot be null");
        }

        try {
            logger.debug("Finding artist by ID: {}", id);

            return artistRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Artist not found with ID: {}", id);
                        return new ArtistNotFoundException(id);
                    });
        } catch (ArtistNotFoundException e) {
            // Just rethrow
            throw e;
        } catch (Exception e) {
            logger.error("Error finding artist by ID: {}", id, e);
            throw new ArtistServiceException("Failed to get artist by ID: " + id, e);
        }
    }

    /**
     * Clears the artist cache.
     * <p>
     * This method delegates to the cache service to clear all cached artist data.
     * It should be called when underlying data might have changed from external
     * sources to ensure the cache stays consistent with the database.
     * <p>
     * The method includes logging and exception handling to track cache clearing
     * operations and issues.
     *
     * @throws ArtistServiceException if there's an error clearing the cache
     */
    public void clearCache() {
        logger.info("Clearing artist cache");
        try {
            cacheService.clearCache();
            logger.info("Artist cache cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing artist cache", e);
            throw new ArtistServiceException("Failed to clear artist cache", e);
        }
    }

    /**
     * Gets statistics about the artist service.
     * <p>
     * This method collects and returns various statistics about the artist module,
     * which can be useful for monitoring and diagnostics. It aggregates information
     * from multiple sources including the repository and cache service.
     * <p>
     * The method includes logging and exception handling to ensure reliable
     * operation when gathering statistics.
     *
     * @return Map of statistic names to their values
     * @throws ArtistServiceException if there's an error gathering statistics
     */
    public Map<String, Object> getStatistics() {
        try {
            logger.debug("Gathering artist service statistics");

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalArtists", artistRepository.count());
            stats.put("cachedArtists", cacheService.getCachedArtistCount());

            logger.debug("Artist statistics: {}", stats);
            return stats;
        } catch (Exception e) {
            logger.error("Error gathering artist statistics", e);
            throw new ArtistServiceException("Failed to get artist statistics", e);
        }
    }
}