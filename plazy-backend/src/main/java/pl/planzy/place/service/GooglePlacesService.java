package pl.planzy.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import pl.planzy.place.domain.Place;
import pl.planzy.place.domain.PlaceRepository;
import pl.planzy.place.exception.GooglePlacesApiException;
import pl.planzy.place.exception.PlaceServiceException;
import reactor.core.publisher.Mono;

import io.github.resilience4j.retry.annotation.Retry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for interacting with the Google Places API.
 * <p>
 * This service is responsible for all communication with the Google Places API,
 * providing methods to search for places and retrieve detailed information. It
 * includes resilience patterns like circuit breakers and retry mechanisms to
 * handle external service failures gracefully.
 * <p>
 * The service implements rate limiting to comply with Google's API usage policies
 * and provides comprehensive error handling and logging for external API interactions.
 */
@Service
public class GooglePlacesService {
    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesService.class);

    private final WebClient webClient;
    private final PlaceRepository placeRepository;
    private final ObjectMapper objectMapper;

    @Value("${google.maps.api.key}")
    private String apiKey;

    @Value("${google.places.api.request.delay:200}")
    private long requestDelayMs;

    // Rate limiting implementation
    private final Object rateLimitLock = new Object();
    private long lastRequestTime = 0;

    /**
     * Constructs a new GooglePlacesService with the required dependencies.
     *
     * @param placeRepository Repository for place entity operations
     * @param objectMapper JSON object mapper for parsing responses
     */
    public GooglePlacesService(PlaceRepository placeRepository, ObjectMapper objectMapper) {
        this.placeRepository = placeRepository;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://maps.googleapis.com/maps/api")
                .build();
    }

    /**
     * Applies rate limiting before making API requests.
     * <p>
     * This method ensures that requests to the Google Places API are spaced
     * appropriately to avoid exceeding rate limits. It uses a synchronization
     * mechanism to ensure accurate timing even with concurrent access.
     */
    private void applyRateLimit() {
        synchronized (rateLimitLock) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRequest = currentTime - lastRequestTime;

            if (timeSinceLastRequest < requestDelayMs) {
                try {
                    Thread.sleep(requestDelayMs - timeSinceLastRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Rate limiting sleep was interrupted");
                }
            }

            lastRequestTime = System.currentTimeMillis();
        }
    }

    /**
     * Find a place ID using the Google Places Text Search API.
     * <p>
     * This method searches for a place based on a name and location, returning
     * the Google Place ID if found. It includes rate limiting, circuit breaking,
     * and retry mechanisms for reliable operation.
     *
     * @param placeName The name of the place to search for
     * @param location The location context for the search
     * @return The Google Place ID if found, or null if no match
     * @throws GooglePlacesApiException if there's an error with the Google Places API
     */
    @CircuitBreaker(name = "googlePlacesApi", fallbackMethod = "getGooglePlaceIdFallback")
    @Retry(name = "googlePlacesApi", fallbackMethod = "getGooglePlaceIdFallback")
    public String getGooglePlaceId(String placeName, String location) {
        if (placeName == null || placeName.trim().isEmpty()) {
            logger.warn("Attempted to get Google Place ID with null or empty place name");
            return null;
        }

        applyRateLimit();

        String query = placeName + " " + (location != null ? location : "");
        logger.info("Searching Google Places API for: {}", query);

        String uri = UriComponentsBuilder
                .fromPath("/place/textsearch/json")
                .queryParam("query", query)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        try {
            JsonNode response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        HttpStatus status = HttpStatus.resolve(clientResponse.statusCode().value());
                        String message = "Error response from Google Places API: " + clientResponse.statusCode();
                        logger.error(message);
                        return Mono.error(new GooglePlacesApiException(message, clientResponse.statusCode().value(), null));
                    })
                    .bodyToMono(JsonNode.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(300))
                            .filter(throwable -> !(throwable instanceof InterruptedException)))
                    .block();

            if (response == null) {
                logger.warn("Null response received from Google Places API for query: {}", query);
                return null;
            }

            // Check for API-level error status
            if (response.has("status")) {
                String status = response.get("status").asText();
                if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                    String errorMessage = "Google Places API error status: " + status;
                    if (response.has("error_message")) {
                        errorMessage += " - " + response.get("error_message").asText();
                    }
                    logger.error(errorMessage + " for query: {}", query);
                    throw new GooglePlacesApiException(errorMessage);
                }
            }

            JsonNode results = response.path("results");
            if (results.isArray() && results.size() > 0) {
                String placeId = results.path(0).path("place_id").asText();
                String foundName = results.path(0).path("name").asText();
                logger.info("Found Google Places result: {} with Place ID: {}", foundName, placeId);
                return placeId;
            } else {
                logger.warn("No Google Places results found for: {}", query);
                return null;
            }

        } catch (GooglePlacesApiException e) {
            // Just rethrow our custom exception
            throw e;
        } catch (Exception e) {
            logger.error("Error searching Google Places API for {}: {}", placeName, e.getMessage(), e);
            throw new GooglePlacesApiException("Failed to search Google Places API: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method for getGooglePlaceId when the API call fails.
     * <p>
     * This method is called by the circuit breaker or retry mechanism when
     * the primary method fails repeatedly. It logs the failure and returns null,
     * allowing the application to continue functioning without the external data.
     *
     * @param placeName The name of the place that was being searched
     * @param location The location context for the search
     * @param e The exception that triggered the fallback
     * @return null, indicating no place ID could be found
     */
    public String getGooglePlaceIdFallback(String placeName, String location, Exception e) {
        logger.warn("Fallback triggered for Google Places API lookup of {} in {}: {}",
                placeName, location, e.getMessage());
        return null;
    }

    /**
     * Enriches a place with details from Google Places API.
     * <p>
     * This method takes a place with a Google Place ID and enriches it with
     * additional data from the Places API, including address, contact information,
     * and ratings. It handles duplicate checking and updates the place with
     * the retrieved data.
     *
     * @param place The place to enrich
     * @return The enriched place
     * @throws GooglePlacesApiException if there's an error with the Google Places API
     * @throws PlaceServiceException if there's an error updating the place
     */
    @CircuitBreaker(name = "googlePlacesApi", fallbackMethod = "enrichPlaceWithGoogleDataFallback")
    @Retry(name = "googlePlacesApi", fallbackMethod = "enrichPlaceWithGoogleDataFallback")
    public Place enrichPlaceWithGoogleData(Place place) {
        if (place == null) {
            logger.warn("Attempted to enrich null place");
            return null;
        }

        if (place.getGooglePlaceId() == null || place.getGooglePlaceId().trim().isEmpty()) {
            logger.warn("Attempted to enrich place without Google Place ID: {}", place.getNameScrapped());
            return place;
        }

        logger.info("Enriching place data for: {}", place.getNameScrapped());
        logger.info("Google Place ID: {}", place.getGooglePlaceId());

        // Check for duplicates first to avoid redundant API calls and data inconsistencies
        Optional<Place> duplicate = placeRepository.findByGooglePlaceId(place.getGooglePlaceId());

        if (duplicate.isPresent()) {
            Place existingPlace = duplicate.get();
            logger.info("Found duplicate Google Place ID for {}. Using the existing record.",
                    place.getNameScrapped());

            // Check if the existing record needs refreshing
            if (existingPlace.getLastEnrichedDate() == null ||
                    existingPlace.getLastEnrichedDate().isBefore(LocalDateTime.now().minusDays(30))) {
                logger.info("Existing place record is stale. Refreshing data from Google Places API");
                return getGooglePlaceDetails(existingPlace);
            }

            return existingPlace;
        }

        return getGooglePlaceDetails(place);
    }

    /**
     * Fallback method for enrichPlaceWithGoogleData when the API call fails.
     * <p>
     * This method is called by the circuit breaker or retry mechanism when
     * the primary method fails repeatedly. It logs the failure and returns
     * the original place object, allowing the application to continue with
     * the data it already has.
     *
     * @param place The place that was being enriched
     * @param e The exception that triggered the fallback
     * @return The original place object, unenriched
     */
    public Place enrichPlaceWithGoogleDataFallback(Place place, Exception e) {
        logger.warn("Fallback triggered for Google Places API enrichment of {}: {}",
                place != null ? place.getNameScrapped() : "null", e.getMessage());

        if (place != null) {
            // Mark the place as having been attempted for enrichment
            place.setLastEnrichedDate(LocalDateTime.now());

            try {
                return placeRepository.save(place);
            } catch (Exception saveEx) {
                logger.error("Error saving place during fallback: {}", saveEx.getMessage(), saveEx);
            }
        }

        return place;
    }

    /**
     * Fetches detailed place information from Google Places API.
     * <p>
     * This method makes a call to the Google Places Details API to retrieve
     * comprehensive information about a place. It handles the parsing of the
     * response and mapping of Google's data structure to our place entity.
     *
     * @param place The place to retrieve details for
     * @return The place with details added
     * @throws GooglePlacesApiException if there's an error with the Google Places API
     */
    private Place getGooglePlaceDetails(Place place) {
        applyRateLimit();

        String uri = UriComponentsBuilder
                .fromPath("/place/details/json")
                .queryParam("place_id", place.getGooglePlaceId())
                .queryParam("fields", "name,formatted_address,geometry,address_component," +
                        "formatted_phone_number,website,rating,user_ratings_total," +
                        "price_level,type,photo,review,opening_hours")
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        try {
            JsonNode response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        HttpStatus status = HttpStatus.resolve(clientResponse.statusCode().value());
                        String message = "Error response from Google Places API Details: " + clientResponse.statusCode();
                        logger.error(message);
                        return Mono.error(new GooglePlacesApiException(message, clientResponse.statusCode().value(), null));
                    })
                    .bodyToMono(JsonNode.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(300))
                            .filter(throwable -> !(throwable instanceof InterruptedException)))
                    .block();

            if (response == null) {
                logger.error("Null response from Google Places API for place ID: {}", place.getGooglePlaceId());
                throw new GooglePlacesApiException("Null response from Google Places API");
            }

            // Check for API-level error status
            if (response.has("status")) {
                String status = response.get("status").asText();
                if (!"OK".equals(status)) {
                    String errorMessage = "Google Places API error status: " + status;
                    if (response.has("error_message")) {
                        errorMessage += " - " + response.get("error_message").asText();
                    }
                    logger.error(errorMessage);
                    throw new GooglePlacesApiException(errorMessage);
                }
            }

            if (!response.has("result")) {
                logger.error("Invalid response from Google Places API - missing 'result' field");
                throw new GooglePlacesApiException("Invalid response from Google Places API - missing 'result' field");
            }

            JsonNode result = response.path("result");

            // Update place with Google data
            place.setNameGoogle(getTextValue(result, "name"));
            place.setFormattedAddress(getTextValue(result, "formatted_address"));
            place.setWebsite(getTextValue(result, "website"));
            place.setPhoneNumber(getTextValue(result, "formatted_phone_number"));

            // Handle rating and user ratings
            if (!result.path("rating").isMissingNode()) {
                place.setRating(result.path("rating").asDouble());
            }

            if (!result.path("user_ratings_total").isMissingNode()) {
                place.setUserRatingsTotal(result.path("user_ratings_total").asInt());
            }

            // Calculate and set popularity score
            double popularityScore = calculatePopularity(
                    place.getRating() != null ? place.getRating() : 0.0,
                    place.getUserRatingsTotal() != null ? place.getUserRatingsTotal() : 0);
            place.setPopularityScore(popularityScore);

            // Set price level
            if (!result.path("price_level").isMissingNode()) {
                place.setPriceLevel(result.path("price_level").asInt());
            }

            // Set coordinates
            if (!result.path("geometry").isMissingNode() && !result.path("geometry").path("location").isMissingNode()) {
                JsonNode location = result.path("geometry").path("location");
                place.setLatitude(location.path("lat").asDouble());
                place.setLongitude(location.path("lng").asDouble());
            }

            // Store place types
            if (result.has("types") && result.path("types").isArray()) {
                List<String> types = new ArrayList<>();
                for (JsonNode type : result.path("types")) {
                    types.add(type.asText());
                }
                place.setPlaceTypes(String.join(",", types));
            }

            // Extract primary photo reference if available
            if (result.has("photos") && result.path("photos").isArray() && result.path("photos").size() > 0) {
                String photoReference = result.path("photos").path(0).path("photo_reference").asText();
                place.setPrimaryPhotoReference(safelyTruncate(photoReference, 1990)); // Ensure it fits in the database column
            }

            // Extract reviews count if available
            if (result.has("reviews") && result.path("reviews").isArray()) {
                place.setReviewCount(result.path("reviews").size());
            }

            // Extract address components
            if (result.has("address_components") && result.path("address_components").isArray()) {
                extractAddressComponents(place, result.path("address_components"));
            }

            place.setLastEnrichedDate(LocalDateTime.now());

            // Save updated place
            place = placeRepository.save(place);

            logger.info("Successfully enriched and saved place data for: {}", place.getNameGoogle());
            return place;

        } catch (GooglePlacesApiException e) {
            // Just rethrow our custom exception
            throw e;
        } catch (Exception e) {
            logger.error("Error enriching place data for {}: {}", place.getNameScrapped(), e.getMessage(), e);
            throw new GooglePlacesApiException("Failed to get place details from Google Places API", e);
        }
    }

    /**
     * Extract address components from Google Places API response.
     * <p>
     * This method parses the address_components field from the API response and
     * maps the various components to our place entity structure.
     *
     * @param place The place to update with address components
     * @param addressComponents The address components from the API response
     */
    private void extractAddressComponents(Place place, JsonNode addressComponents) {
        for (JsonNode component : addressComponents) {
            if (component.has("types") && component.path("types").isArray()) {
                String type = component.path("types").path(0).asText();
                String longName = component.path("long_name").asText();

                switch (type) {
                    case "locality":
                        place.setCity(longName);
                        break;
                    case "country":
                        place.setCountry(longName);
                        break;
                    case "postal_code":
                        place.setPostalCode(longName);
                        break;
                    case "administrative_area_level_1":
                        place.setState(longName);
                        break;
                    case "sublocality":
                    case "sublocality_level_1":
                        place.setNeighborhood(longName);
                        break;
                    case "route":
                        place.setStreet(longName);
                        break;
                    case "street_number":
                        place.setStreetNumber(longName);
                        break;
                }
            }
        }
    }

    /**
     * Calculate popularity score based on rating and number of reviews.
     * <p>
     * This method computes a normalized popularity score for a place based on its
     * rating and the number of user reviews. The algorithm uses a Bayesian average
     * approach to adjust for confidence based on sample size.
     *
     * @param rating The place's rating (0-5 scale)
     * @param userRatingsTotal The number of user ratings/reviews
     * @return A popularity score between 0-100
     */
    public double calculatePopularity(double rating, int userRatingsTotal) {
        // Constants for tuning the algorithm
        final double MAX_RATING = 5.0;
        final double RATINGS_FOR_FULL_CONFIDENCE = 500.0;

        // Normalize the rating to 0-1 scale
        double normalizedRating = rating / MAX_RATING;

        // Calculate confidence factor (how much we trust the rating based on sample size)
        double confidenceFactor = Math.min(1.0,
                Math.log(1 + userRatingsTotal) / Math.log(1 + RATINGS_FOR_FULL_CONFIDENCE));

        // Apply Bayesian average with global mean (assuming average rating is 4.0/5.0)
        double globalMeanNormalized = 4.0 / MAX_RATING;
        double bayesianAverage = (normalizedRating * confidenceFactor) +
                (globalMeanNormalized * (1 - confidenceFactor));

        // Calculate popularity boost from number of ratings (caps at RATINGS_FOR_FULL_CONFIDENCE)
        double quantityBoost = Math.min(1.0,
                Math.log(1 + userRatingsTotal) / Math.log(1 + RATINGS_FOR_FULL_CONFIDENCE));

        // Combine quality and quantity into final score (0-100 scale)
        return (bayesianAverage * 0.7 + quantityBoost * 0.3) * 100;
    }

    /**
     * Safely get a text value from a JsonNode.
     * <p>
     * This helper method safely extracts a text value from a JsonNode,
     * handling null cases and missing nodes gracefully.
     *
     * @param node The JsonNode to extract from
     * @param fieldName The field name to extract
     * @return The text value or null if not found/null
     */
    private String getTextValue(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.path(fieldName).isNull()) {
            return null;
        }
        return node.path(fieldName).asText();
    }

    /**
     * Safely truncate a string to the specified maximum length.
     * <p>
     * This helper method ensures that strings don't exceed the maximum length
     * allowed by database columns, preventing potential data truncation errors.
     *
     * @param value The string to truncate
     * @param maxLength The maximum allowed length
     * @return The truncated string or null if the input was null
     */
    private String safelyTruncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}