package pl.planzy.artist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pl.planzy.artist.domain.Artist;
import pl.planzy.artist.exception.ArtistServiceException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for executing complex database queries related to artists.
 * <p>
 * This service provides optimized database access methods for artist-related
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
public class ArtistQueryService {
    private static final Logger logger = LoggerFactory.getLogger(ArtistQueryService.class);

    /**
     * JDBC template for database access.
     * Provides methods for executing SQL queries and updates.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs a new ArtistQueryService with the required dependencies.
     *
     * @param jdbcTemplate JDBC template for database access
     */
    @Autowired
    public ArtistQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds artists by their names using a single optimized SQL query.
     * <p>
     * This method efficiently retrieves multiple artists in a single database
     * query using an SQL IN clause. It's significantly more efficient than
     * making separate queries for each artist name, especially when looking
     * up a large number of artists.
     * <p>
     * The method includes validation, filtering, comprehensive logging, and
     * exception handling to ensure reliable operation and troubleshooting
     * in production environments.
     *
     * @param names Set of artist names to search for
     * @return List of Artist entities matching the provided names
     * @throws ArtistServiceException if there's an error executing the query
     */
    public List<Artist> findArtistsByNames(Set<String> names) {
        if (names == null) {
            logger.warn("Called findArtistsByNames with null names set");
            return Collections.emptyList();
        }

        if (names.isEmpty()) {
            logger.debug("Called findArtistsByNames with empty names set");
            return Collections.emptyList();
        }

        logger.info("Finding artists by names. Count: {}", names.size());

        try {
            // Filter out null or empty names
            Set<String> validNames = names.stream()
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toSet());

            if (validNames.isEmpty()) {
                logger.warn("No valid names to search for after filtering");
                return Collections.emptyList();
            }

            // Prepare parameters for the SQL IN clause
            StringBuilder placeholders = new StringBuilder();
            List<String> orderedNames = new ArrayList<>(validNames);

            for (int i = 0; i < orderedNames.size(); i++) {
                placeholders.append("?");
                if (i < orderedNames.size() - 1) {
                    placeholders.append(",");
                }
            }

            // SQL query with IN clause - this approach is more efficient than individual queries
            String sql = "SELECT id, artist_name FROM artists WHERE artist_name IN (" + placeholders + ")";

            logger.debug("Executing SQL to find artists by names: {}", sql);

            List<Artist> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Artist artist = new Artist();
                artist.setId(rs.getLong("id"));
                artist.setArtistName(rs.getString("artist_name"));
                return artist;
            }, orderedNames.toArray());

            logger.info("Found {} artists out of {} requested names", results.size(), validNames.size());

            // Log details about missing artists to help diagnose issues
            if (results.size() < validNames.size() && logger.isDebugEnabled()) {
                Set<String> foundNames = results.stream()
                        .map(Artist::getArtistName)
                        .collect(Collectors.toSet());

                validNames.stream()
                        .filter(name -> !foundNames.contains(name))
                        .forEach(name -> logger.debug("Artist not found: {}", name));
            }

            return results;

        } catch (DataAccessException e) {
            logger.error("Database error while finding artists by names", e);
            throw new ArtistServiceException("Failed to find artists by names", e);
        } catch (Exception e) {
            logger.error("Unexpected error while finding artists by names", e);
            throw new ArtistServiceException("Unexpected error finding artists by names", e);
        }
    }
}