package pl.planzy.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.planzy.place.domain.Place;
import pl.planzy.place.domain.PlaceRepository;
import pl.planzy.place.exception.GooglePlacesApiException;
import pl.planzy.place.exception.PlaceNotFoundException;
import pl.planzy.place.exception.PlaceServiceException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Primary service for place-related operations.
 * <p>
 * This service provides the main entry point for place management operations,
 * coordinating between the repository, specialized services, and external APIs
 * to implement the core business logic for places. It follows a modular design
 * pattern where each specialized service handles a specific responsibility.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Retrieving and creating places</li>
 *   <li>Enriching places with data from external services</li>
 *   <li>Coordinating caching operations for performance</li>
 *   <li>Managing scheduled refreshes of place data</li>
 *   <li>Providing consolidated statistics</li>
 * </ul>
 * <p>
 * The service includes comprehensive logging and exception handling to ensure
 * reliable operation and facilitate troubleshooting in production environments.
 */
@Service
public class PlaceService {
    private static final Logger logger = LoggerFactory.getLogger(PlaceService.class);

    private final PlaceRepository placeRepository;
    private final PlaceCacheService cacheService;
    private final PlaceQueryService queryService;
    private final GooglePlacesService googlePlacesService;

    // Configuration settings
    @Value("${google.places.enrichment.enabled:false}")
    private boolean placesEnrichmentEnabled;

    @Value("${google.places.enrichment.refresh.days:30}")
    private int refreshDays;

    /**
     * Constructs a new PlaceService with the required dependencies.
     *
     * @param placeRepository Repository for place entity operations
     * @param cacheService Service for place caching operations
     * @param queryService Service for complex place queries
     * @param googlePlacesService Service for Google Places API interactions
     */
    @Autowired
    public PlaceService(
            PlaceRepository placeRepository,
            PlaceCacheService cacheService,
            PlaceQueryService queryService,
            GooglePlacesService googlePlacesService) {
        this.placeRepository = placeRepository;
        this.cacheService = cacheService;
        this.queryService = queryService;
        this.googlePlacesService = googlePlacesService;

        logger.info("PlaceService initialized. Google Places enrichment is {}",
                placesEnrichmentEnabled ? "enabled" : "disabled");
    }

    /**
     * Gets or creates a place based on event data.
     * <p>
     * This method processes place information from event data, looking up existing
     * places or creating new ones as needed. If enabled, it also enriches places
     * with data from the Google Places API.
     * <p>
     * The method is designed to be resilient to external API failures, using
     * circuit breaker and retry patterns to handle issues gracefully.
     *
     * @param eventNode JSON data containing place information
     * @return The place entity, or null if place information is missing/invalid
     * @throws PlaceServiceException if there's an error during processing
     */
    @CircuitBreaker(name = "placeService", fallbackMethod = "findOrCreatePlaceFromEventDataFallback")
    @Retry(name = "placeService", fallbackMethod = "findOrCreatePlaceFromEventDataFallback")
    @Transactional(propagation = Propagation.REQUIRED)
    public Place findOrCreatePlaceFromEventData(JsonNode eventNode) {
        if (!placesEnrichmentEnabled) {
            logger.debug("Place enrichment is disabled, skipping place processing");
            return null;
        }

        if (eventNode == null) {
            logger.warn("Null event data provided to findOrCreatePlaceFromEventData");
            return null;
        }

        // Extract place information from event data
        String placeName = eventNode.has("place") ? eventNode.get("place").asText() : null;
        String location = eventNode.has("location") ? eventNode.get("location").asText() : null;

        if (placeName == null || placeName.trim().isEmpty()) {
            logger.debug("No place name found in event data, skipping place processing");
            return null;
        }

        logger.info("Processing place: {} at location: {}", placeName, location);

        try {
            // First check the cache for an existing Google Place ID
            String cachedPlaceId = cacheService.getPlaceIdByLocation(placeName, location);

            if (cachedPlaceId != null) {
                Optional<Place> existingPlace = placeRepository.findByGooglePlaceId(cachedPlaceId);

                if (existingPlace.isPresent()) {
                    Place place = existingPlace.get();
                    logger.debug("Found existing place in cache: {} (ID: {})", place.getNameScrapped(), place.getGooglePlaceId());

                    // Check if the place needs refreshing
                    if (requiresRefresh(place)) {
                        try {
                            place = googlePlacesService.enrichPlaceWithGoogleData(place);
                            logger.info("Refreshed Google data for place: {}", place.getNameScrapped());
                        } catch (GooglePlacesApiException e) {
                            logger.warn("Failed to refresh Google data for place: {}", place.getNameScrapped(), e);
                            // Continue with existing data
                        }
                    }

                    return place;
                } else {
                    // Place ID is in cache but not in database, remove from cache
                    logger.warn("Cached Google Place ID: {} not found in database", cachedPlaceId);
                }
            }

            // No cache hit or invalid cache, try to get Place ID from Google
            String googlePlaceId = null;

            try {
                googlePlaceId = googlePlacesService.getGooglePlaceId(placeName, location);
            } catch (GooglePlacesApiException e) {
                logger.warn("Error getting Google Place ID for {}: {}", placeName, e.getMessage());
                // Continue without Google data
            }

            if (googlePlaceId != null) {
                // Cache the Google Place ID for future lookups
                cacheService.cachePlaceIdByLocation(placeName, location, googlePlaceId);

                // Check if a place with this Google ID already exists
                Optional<Place> existingPlace = placeRepository.findByGooglePlaceId(googlePlaceId);

                if (existingPlace.isPresent()) {
                    Place place = existingPlace.get();
                    logger.debug("Found existing place by Google Place ID: {} (Name: {})",
                            googlePlaceId, place.getNameScrapped());
                    return place;
                }

                // Create a new place with Google Place ID
                Place newPlace = new Place();
                newPlace.setNameScrapped(placeName);
                newPlace.setGooglePlaceId(googlePlaceId);

                // Enrich with Google data
                try {
                    Place enrichedPlace = googlePlacesService.enrichPlaceWithGoogleData(newPlace);
                    logger.info("Created and enriched new place: {}", enrichedPlace.getNameScrapped());
                    return enrichedPlace;
                } catch (GooglePlacesApiException e) {
                    logger.warn("Failed to enrich new place with Google data: {}", e.getMessage());
                    // Save what we have without enrichment
                    newPlace.setLastEnrichedDate(LocalDateTime.now());
                    Place savedPlace = placeRepository.save(newPlace);
                    logger.info("Created new place without enrichment: {}", savedPlace.getNameScrapped());
                    return savedPlace;
                }
            } else {
                // No Google Place ID found, create a minimal place record
                logger.info("No Google Place ID found for: {}. Creating minimal place record", placeName);

                Place minimalPlace = new Place();
                minimalPlace.setNameScrapped(placeName);
                minimalPlace.setLastEnrichedDate(LocalDateTime.now());

                if (location != null && !location.trim().isEmpty()) {
                    minimalPlace.setCity(location);
                }

                Place savedPlace = placeRepository.save(minimalPlace);
                logger.info("Created minimal place record: {}", savedPlace.getNameScrapped());
                return savedPlace;
            }

        } catch (Exception e) {
            logger.error("Error processing place for event: {}", placeName, e);
            throw new PlaceServiceException("Failed to process place: " + placeName, e);
        }
    }

    /**
     * Fallback method for findOrCreatePlaceFromEventData when the operation fails.
     * <p>
     * This method is called by the circuit breaker or retry mechanism when
     * the primary method fails repeatedly. It logs the failure and returns null,
     * allowing the application to continue without place data.
     *
     * @param eventNode JSON data containing place information
     * @param e The exception that triggered the fallback
     * @return null, indicating no place could be created/found
     */
    public Place findOrCreatePlaceFromEventDataFallback(JsonNode eventNode, Exception e) {
        String placeName = eventNode != null && eventNode.has("place") ?
                eventNode.get("place").asText() : "unknown";

        logger.warn("Fallback triggered for place processing of {}: {}", placeName, e.getMessage());
        return null;
    }

    /**
     * Gets a place by its Google Place ID.
     * <p>
     * This method retrieves a place entity by its Google Place ID, throwing
     * an exception if the place doesn't exist.
     *
     * @param googlePlaceId The Google Place ID
     * @return The place entity
     * @throws PlaceNotFoundException if the place doesn't exist
     * @throws PlaceServiceException if there's an unexpected error
     */
    @Transactional(readOnly = true)
    public Place getPlaceByGoogleId(String googlePlaceId) {
        if (googlePlaceId == null || googlePlaceId.trim().isEmpty()) {
            logger.warn("Attempted to get place with null or empty Google Place ID");
            throw new IllegalArgumentException("Google Place ID cannot be null or empty");
        }

        try {
            logger.debug("Finding place by Google Place ID: {}", googlePlaceId);

            return placeRepository.findByGooglePlaceId(googlePlaceId)
                    .orElseThrow(() -> {
                        logger.warn("Place not found with Google Place ID: {}", googlePlaceId);
                        return new PlaceNotFoundException(googlePlaceId);
                    });
        } catch (PlaceNotFoundException e) {
            // Just rethrow
            throw e;
        } catch (Exception e) {
            logger.error("Error finding place by Google Place ID: {}", googlePlaceId, e);
            throw new PlaceServiceException("Failed to get place by Google Place ID: " + googlePlaceId, e);
        }
    }

    /**
     * Determines if a place requires data refreshing from external sources.
     * <p>
     * This method checks if a place has never been enriched or if its data
     * is older than the configured refresh period.
     *
     * @param place The place to check
     * @return true if the place needs refreshing, false otherwise
     */
    private boolean requiresRefresh(Place place) {
        if (place.getLastEnrichedDate() == null) {
            return true;
        }

        LocalDateTime refreshThreshold = LocalDateTime.now().minusDays(refreshDays);
        return place.getLastEnrichedDate().isBefore(refreshThreshold);
    }

    /**
     * Scheduled task to refresh outdated place data.
     * <p>
     * This method runs on a schedule to identify places with stale data and
     * refresh them from external sources, ensuring the database stays current.
     */
    @Scheduled(cron = "${google.places.refresh.cron:0 0 3 * * ?}") // Default: 3 AM daily
    @Transactional
    public void refreshOutdatedPlaces() {
        if (!placesEnrichmentEnabled) {
            logger.info("Places enrichment is disabled, skipping refresh task");
            return;
        }

        logger.info("Starting places refresh task");

        try {
            LocalDateTime refreshThreshold = LocalDateTime.now().minusDays(refreshDays);
            List<Place> outdatedPlaces = queryService.findPlacesNeedingEnrichment(refreshThreshold, 100);

            logger.info("Found {} places needing data refresh", outdatedPlaces.size());

            int successCount = 0;
            int errorCount = 0;

            for (Place place : outdatedPlaces) {
                try {
                    googlePlacesService.enrichPlaceWithGoogleData(place);
                    successCount++;
                    logger.debug("Successfully refreshed place: {} (ID: {})",
                            place.getNameScrapped(), place.getGooglePlaceId());
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error refreshing place: {} (ID: {}): {}",
                            place.getNameScrapped(), place.getGooglePlaceId(), e.getMessage());
                }
            }

            logger.info("Places refresh task completed. Success: {}, Errors: {}",
                    successCount, errorCount);

        } catch (Exception e) {
            logger.error("Error during places refresh task", e);
        }
    }

    /**
     * Clears the place cache.
     * <p>
     * This method delegates to the cache service to clear all cached place data.
     * It should be called when underlying data might have changed from external
     * sources to ensure the cache stays consistent with the database.
     */
    public void clearCache() {
        logger.info("Clearing place cache");
        cacheService.clearCache();
    }

    /**
     * Gets statistics about the place service.
     * <p>
     * This method collects and returns various statistics about the place module,
     * which can be useful for monitoring and diagnostics.
     *
     * @return Map of statistic names to their values
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlaces", placeRepository.count());
        stats.put("cachedPlaces", cacheService.getCachedPlaceCount());
        stats.put("enrichmentEnabled", placesEnrichmentEnabled);
        stats.put("refreshIntervalDays", refreshDays);
        return stats;
    }
}