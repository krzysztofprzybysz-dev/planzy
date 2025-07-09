package pl.planzy.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.common.util.StringUtils;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serwis odpowiedzialny za integrację miejsc z Google Places API.
 * Implementuje wzorce Circuit Breaker i Retry dla obsługi błędów API.
 */
@Service
public class PlaceIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(PlaceIntegrationService.class);

    private final PlaceRepository placeRepository;
    private final GooglePlacesService googlePlacesService;

    // Cache miejsc po ID dla zwiększenia wydajności
    private final Map<String, String> placeIdCache = new ConcurrentHashMap<>();

    @Value("${google.places.enrichment.enabled:false}")
    private boolean placesEnrichmentEnabled;

    @Value("${google.places.enrichment.refresh.days:30}")
    private int refreshDays;

    @Autowired
    public PlaceIntegrationService(PlaceRepository placeRepository, GooglePlacesService googlePlacesService) {
        this.placeRepository = placeRepository;
        this.googlePlacesService = googlePlacesService;

        logger.info("PlaceIntegrationService initialized. Google Places enrichment is {}",
                placesEnrichmentEnabled ? "enabled" : "disabled");
    }

    /**
     * Pobiera istniejące lub tworzy nowe miejsce dla wydarzenia.
     * Używa wzorca Circuit Breaker dla bezpiecznego wywołania Google Places API.
     */
    @CircuitBreaker(name = "googlePlacesService", fallbackMethod = "getDefaultPlace")
    @Retry(name = "googlePlacesService", fallbackMethod = "getDefaultPlace")
    @Transactional(propagation = Propagation.REQUIRED)
    public Place getOrCreatePlace(JsonNode eventNode) {
        if (!placesEnrichmentEnabled) {
            return null; // Pomijamy wzbogacanie miejsca, jeśli wyłączone
        }

        // Pobieramy dane miejsca
        String placeName = eventNode.has("place") ? eventNode.get("place").asText() : null;
        String location = eventNode.has("location") ? eventNode.get("location").asText() : null;

        // Wczesny powrót, jeśli brak nazwy miejsca
        if (StringUtils.isBlank(placeName)) {
            logger.debug("No place name found for event: {}",
                    eventNode.has("event_name") ? eventNode.get("event_name").asText() : "Unknown Event");
            return null;
        }

        // Łączymy nazwę i lokalizację jako klucz cache
        String cacheKey = placeName + "|" + location;

        // Sprawdzamy najpierw w cache
        String cachedPlaceId = placeIdCache.get(cacheKey);
        if (cachedPlaceId != null) {
            Optional<Place> existingPlace = placeRepository.findByGooglePlaceId(cachedPlaceId);
            if (existingPlace.isPresent()) {
                return existingPlace.get();
            }
        }

        try {
            // Pobieramy ID miejsca z Google Places API
            String googlePlaceId = googlePlacesService.getGooglePlaceId(placeName, location);

            if (googlePlaceId == null) {
                logger.warn("No Google Place ID found for: {}", placeName);
                return null;
            }

            // Sprawdzamy czy miejsce już istnieje w bazie
            Optional<Place> existingPlace = placeRepository.findByGooglePlaceId(googlePlaceId);
            if (existingPlace.isPresent()) {
                Place place = existingPlace.get();
                placeIdCache.put(cacheKey, place.getGooglePlaceId());

                // Sprawdzamy czy miejsce wymaga odświeżenia danych
                if (requiresRefresh(place)) {
                    try {
                        place = googlePlacesService.enrichPlaceWithGoogleData(place);
                        place = placeRepository.save(place);
                        logger.info("Refreshed Google data for place: {}", place.getNameScrapped());
                    } catch (Exception e) {
                        logger.warn("Failed to refresh Google data for place: {}", place.getNameScrapped(), e);
                    }
                }

                return place;
            }

            // Tworzymy nowe miejsce
            Place place = new Place();
            place.setNameScrapped(placeName);
            place.setGooglePlaceId(googlePlaceId);

            // Wzbogacamy danymi z Google
            Place enrichedPlace = googlePlacesService.enrichPlaceWithGoogleData(place);
            Place savedPlace = placeRepository.save(enrichedPlace);

            // Dodajemy do cache
            placeIdCache.put(cacheKey, savedPlace.getGooglePlaceId());

            return savedPlace;

        } catch (Exception e) {
            logger.error("Error assigning place for event: {}", placeName, e);
            return null;
        }
    }

    /**
     * Metoda fallback dla Circuit Breaker - wywołana gdy Google Places API zawiedzie.
     */
    private Place getDefaultPlace(JsonNode eventNode, Exception e) {
        logger.warn("Circuit breaker triggered for Google Places API: {}",
                e.getMessage());

        // W przypadku błędu zwracamy null zamiast próbować stworzyć miejsce
        return null;
    }

    /**
     * Sprawdza, czy miejsce wymaga odświeżenia danych z Google Places API.
     */
    private boolean requiresRefresh(Place place) {
        if (place.getLastEnrichedDate() == null) {
            return true;
        }

        LocalDateTime refreshThreshold = LocalDateTime.now().minusDays(refreshDays);
        return place.getLastEnrichedDate().isBefore(refreshThreshold);
    }

    /**
     * Zadanie cykliczne do odświeżania danych miejsc.
     */
    @Scheduled(cron = "${google.places.refresh.cron:0 0 3 * * ?}") // Domyślnie o 3:00 AM codziennie
    @Transactional
    public void refreshOutdatedPlaces() {
        if (!placesEnrichmentEnabled) {
            logger.info("Places enrichment is disabled, skipping refresh task");
            return;
        }

        logger.info("Starting places refresh task");

        LocalDateTime refreshThreshold = LocalDateTime.now().minusDays(refreshDays);
        placeRepository.findPlacesNeedingEnrichment(refreshThreshold)
                .forEach(place -> {
                    try {
                        Place enrichedPlace = googlePlacesService.enrichPlaceWithGoogleData(place);
                        placeRepository.save(enrichedPlace);
                        logger.info("Refreshed place data for: {}", place.getNameScrapped());
                    } catch (Exception e) {
                        logger.error("Error refreshing place data for {}: {}", place.getNameScrapped(), e.getMessage(), e);
                    }
                });

        logger.info("Places refresh task completed");
    }


    /**
     * Czyści cache miejsc.
     */
    public void clearCache() {
        placeIdCache.clear();
        logger.info("Place cache cleared");
    }

    /**
     * Zwraca statystyki serwisu.
     */

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlaces", placeRepository.count());
        stats.put("cachedPlaces", placeIdCache.size());
        stats.put("enrichmentEnabled", placesEnrichmentEnabled);
        return stats;
    }
}