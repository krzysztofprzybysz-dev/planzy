package pl.planzy.place.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pl.planzy.place.domain.Place;
import pl.planzy.place.exception.PlaceServiceException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Service for executing complex database queries related to places.
 * <p>
 * This service provides optimized database access methods for place-related
 * operations, particularly for batch processing and complex lookups that
 * would be inefficient with standard JPA repository methods.
 * <p>
 * The service uses JDBC directly for some operations to:
 * <ul>
 *   <li>Optimize query performance with specialized SQL</li>
 *   <li>Support batch processing for improved throughput</li>
 *   <li>Execute complex queries not easily expressed with JPA</li>
 *   <li>Provide finer control over the SQL execution</li>
 * </ul>
 * <p>
 * The service includes comprehensive logging and error handling to facilitate
 * troubleshooting database-related issues in production environments.
 */
@Service
public class PlaceQueryService {
    private static final Logger logger = LoggerFactory.getLogger(PlaceQueryService.class);

    /**
     * JDBC template for database access.
     * Provides methods for executing SQL queries and updates.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs a new PlaceQueryService with the required dependencies.
     *
     * @param jdbcTemplate JDBC template for database access
     */
    @Autowired
    public PlaceQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds places that need data enrichment from external API.
     * <p>
     * This method identifies places that either have never been enriched with
     * external data or have not been refreshed within the specified timeframe.
     * This is useful for scheduling periodic refreshes of place data.
     *
     * @param thresholdDate Date before which places are considered to need refreshing
     * @param limit Maximum number of places to return
     * @return List of places needing enrichment
     * @throws PlaceServiceException if there's an error executing the query
     */
    public List<Place> findPlacesNeedingEnrichment(LocalDateTime thresholdDate, int limit) {
        if (thresholdDate == null) {
            logger.warn("Called findPlacesNeedingEnrichment with null threshold date");
            return Collections.emptyList();
        }

        logger.info("Finding places needing enrichment before: {}, limit: {}", thresholdDate, limit);

        try {
            String sql = "SELECT * FROM places WHERE last_enriched_date IS NULL OR last_enriched_date < ? ORDER BY last_enriched_date ASC NULLS FIRST LIMIT ?";

            List<Place> results = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        Place place = new Place();
                        place.setGooglePlaceId(rs.getString("place_id"));
                        place.setNameScrapped(rs.getString("place_name_scrapped"));
                        place.setNameGoogle(rs.getString("place_name_google"));
                        place.setFormattedAddress(rs.getString("formatted_address"));
                        place.setLatitude(rs.getObject("latitude", Double.class));
                        place.setLongitude(rs.getObject("longitude", Double.class));
                        place.setCity(rs.getString("city"));
                        place.setCountry(rs.getString("country"));
                        place.setLastEnrichedDate(rs.getObject("last_enriched_date", LocalDateTime.class));
                        // Additional fields would be mapped here as needed
                        return place;
                    },
                    thresholdDate, limit
            );

            logger.info("Found {} places needing enrichment", results.size());
            return results;

        } catch (DataAccessException e) {
            logger.error("Database error while finding places needing enrichment", e);
            throw new PlaceServiceException("Failed to find places needing enrichment", e);
        } catch (Exception e) {
            logger.error("Unexpected error while finding places needing enrichment", e);
            throw new PlaceServiceException("Unexpected error finding places needing enrichment", e);
        }
    }

    /**
     * Finds popular places in a specific city.
     * <p>
     * This method retrieves places with high popularity scores in a given city,
     * which can be useful for recommendations or featured content.
     *
     * @param city The city to search in
     * @param limit Maximum number of places to return
     * @return List of popular places in the specified city
     * @throws PlaceServiceException if there's an error executing the query
     */
    public List<Place> findPopularPlacesByCity(String city, int limit) {
        if (city == null || city.trim().isEmpty()) {
            logger.warn("Called findPopularPlacesByCity with null or empty city");
            return Collections.emptyList();
        }

        logger.info("Finding popular places in city: {}, limit: {}", city, limit);

        try {
            String sql = "SELECT * FROM places WHERE city = ? AND popularity_score IS NOT NULL " +
                    "ORDER BY popularity_score DESC LIMIT ?";

            List<Place> results = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        Place place = new Place();
                        place.setGooglePlaceId(rs.getString("place_id"));
                        place.setNameScrapped(rs.getString("place_name_scrapped"));
                        place.setNameGoogle(rs.getString("place_name_google"));
                        place.setFormattedAddress(rs.getString("formatted_address"));
                        place.setLatitude(rs.getObject("latitude", Double.class));
                        place.setLongitude(rs.getObject("longitude", Double.class));
                        place.setCity(rs.getString("city"));
                        place.setCountry(rs.getString("country"));
                        place.setPopularityScore(rs.getObject("popularity_score", Double.class));
                        place.setRating(rs.getObject("rating", Double.class));
                        place.setUserRatingsTotal(rs.getObject("user_ratings_total", Integer.class));
                        // Additional fields would be mapped here as needed
                        return place;
                    },
                    city, limit
            );

            logger.info("Found {} popular places in city: {}", results.size(), city);
            return results;

        } catch (DataAccessException e) {
            logger.error("Database error while finding popular places in city: {}", city, e);
            throw new PlaceServiceException("Failed to find popular places in city: " + city, e);
        } catch (Exception e) {
            logger.error("Unexpected error while finding popular places in city: {}", city, e);
            throw new PlaceServiceException("Unexpected error finding popular places in city: " + city, e);
        }
    }
}