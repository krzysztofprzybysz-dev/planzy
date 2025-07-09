package pl.planzy.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Place entities.
 * <p>
 * This interface provides standard CRUD operations for Place entities,
 * leveraging Spring Data JPA to automatically generate the implementation
 * at runtime. It extends JpaRepository to inherit common methods like:
 * <ul>
 *   <li>save() - Create or update a place</li>
 *   <li>findById() - Find a place by ID</li>
 *   <li>findAll() - Retrieve all places</li>
 *   <li>delete() - Delete a place</li>
 *   <li>count() - Count the number of places</li>
 * </ul>
 * <p>
 * The repository serves as the data access layer for places, abstracting
 * away the details of database interactions. It works with both Place entities
 * and their String ID type (Google Place ID).
 * <p>
 * This interface includes custom query methods for specialized place lookups
 * beyond the standard CRUD operations.
 */
public interface PlaceRepository extends JpaRepository<Place, String> {

    /**
     * Finds a place by its Google Place ID.
     * <p>
     * This method provides a way to look up places using the unique identifier
     * from Google Places API, which serves as the primary key for the entity.
     *
     * @param googlePlaceId The Google Place ID to search for
     * @return An Optional containing the place if found, or empty if not found
     */
    Optional<Place> findByGooglePlaceId(String googlePlaceId);

    /**
     * Finds places that need data enrichment or refreshing.
     * <p>
     * This query identifies places that either have never been enriched with
     * external data (lastEnrichedDate is null) or have not been refreshed
     * within the specified timeframe. This is useful for scheduling periodic
     * refreshes of place data from external APIs.
     *
     * @param thresholdDate Date before which places are considered to need refreshing
     * @return List of places needing enrichment
     */
    @Query("SELECT p FROM Place p WHERE p.lastEnrichedDate IS NULL OR p.lastEnrichedDate < :thresholdDate")
    List<Place> findPlacesNeedingEnrichment(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * Finds places by city, ordered by popularity.
     * <p>
     * This method is useful for retrieving popular venues in a specific location,
     * which can be used for recommendations and featured content.
     *
     * @param city The city to search in
     * @return List of places in the specified city, ordered by popularity
     */
    @Query("SELECT p FROM Place p WHERE p.city = :city AND p.popularityScore IS NOT NULL ORDER BY p.popularityScore DESC")
    List<Place> findPlacesByCityOrderByPopularityDesc(@Param("city") String city);
}