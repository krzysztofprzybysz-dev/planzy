package pl.planzy.artist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.planzy.artist.exception.ArtistRelationshipException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing relationships between artists and events.
 * <p>
 * This service provides specialized methods for creating and managing the
 * many-to-many relationship between artists and events. It uses optimized
 * batch operations and direct JDBC access for better performance when
 * dealing with potentially large numbers of relationships.
 * <p>
 * The service handles important aspects of relationship management:
 * <ul>
 *   <li>Efficiently creating many relationships at once with batch operations</li>
 *   <li>Avoiding duplicate relationships to maintain data integrity</li>
 *   <li>Verifying relationship existence with optimized queries</li>
 *   <li>Ensuring transactional integrity during relationship changes</li>
 * </ul>
 * <p>
 * It includes comprehensive logging and exception handling to facilitate
 * troubleshooting relationship issues in production environments.
 */
@Service
public class ArtistRelationshipService {
    private static final Logger logger = LoggerFactory.getLogger(ArtistRelationshipService.class);

    /**
     * JDBC template for database access.
     * Provides methods for executing SQL queries and updates,
     * particularly batch operations.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs a new ArtistRelationshipService with the required dependencies.
     *
     * @param jdbcTemplate JDBC template for database access
     */
    @Autowired
    public ArtistRelationshipService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates relationships between an event and multiple artists.
     * <p>
     * This method efficiently links multiple artists to an event in a single
     * transaction, using batch operations for optimal performance. It checks for
     * existing relationships to avoid duplicates and handle the operation
     * idempotently.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation and facilitate troubleshooting in production environments.
     *
     * @param eventId The ID of the event
     * @param artistIds List of artist IDs to relate to the event
     * @throws ArtistRelationshipException if there's an error creating the relationships
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void linkArtistsToEvent(Long eventId, List<Long> artistIds) {
        if (eventId == null) {
            logger.warn("Attempted to link artists to null event ID");
            return;
        }

        if (artistIds == null || artistIds.isEmpty()) {
            logger.debug("No artists to link to event ID: {}", eventId);
            return;
        }

        logger.info("Linking {} artists to event ID: {}", artistIds.size(), eventId);

        try {
            List<Object[]> eventArtistPairs = new ArrayList<>();
            int skippedCount = 0;

            // First check which relationships already exist to avoid duplicates
            for (Long artistId : artistIds) {
                if (artistId == null) {
                    logger.warn("Null artist ID found in link request for event ID: {}", eventId);
                    skippedCount++;
                    continue;
                }

                if (isArtistLinkedToEvent(eventId, artistId)) {
                    logger.debug("Artist ID: {} already linked to event ID: {}, skipping", artistId, eventId);
                    skippedCount++;
                    continue;
                }

                eventArtistPairs.add(new Object[]{eventId, artistId});
            }

            if (skippedCount > 0) {
                logger.info("Skipped {} artists already linked to event ID: {}", skippedCount, eventId);
            }

            if (eventArtistPairs.isEmpty()) {
                logger.info("No new artist relationships to create for event ID: {}", eventId);
                return;
            }

            // Use batch operation for better performance with multiple relationships
            batchLinkArtistsToEvent(eventArtistPairs);
            logger.info("Successfully linked {} artists to event ID: {}", eventArtistPairs.size(), eventId);

        } catch (ArtistRelationshipException e) {
            // Just re-throw if it's already our custom exception
            throw e;
        } catch (Exception e) {
            logger.error("Error linking artists to event ID: {}", eventId, e);
            throw new ArtistRelationshipException("Failed to link artists to event", e);
        }
    }

    /**
     * Checks if a relationship exists between an event and an artist.
     * <p>
     * This method verifies whether a specific artist is already linked to a given
     * event. It's used both as a standalone method and internally to avoid creating
     * duplicate relationships.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param eventId The ID of the event
     * @param artistId The ID of the artist
     * @return true if the relationship exists, false otherwise
     * @throws ArtistRelationshipException if there's an error checking the relationship
     */
    public boolean isArtistLinkedToEvent(Long eventId, Long artistId) {
        if (eventId == null || artistId == null) {
            logger.warn("Attempted to check relationship with null eventId: {} or artistId: {}", eventId, artistId);
            return false;
        }

        try {
            logger.debug("Checking if artist ID: {} is linked to event ID: {}", artistId, eventId);

            // Using COUNT instead of EXISTS for better compatibility across databases
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM event_artists WHERE event_id = ? AND artist_id = ?",
                    Integer.class, eventId, artistId);

            boolean isLinked = count != null && count > 0;
            logger.debug("Artist ID: {} is {} linked to event ID: {}", artistId, isLinked ? "already" : "not", eventId);

            return isLinked;

        } catch (DataAccessException e) {
            logger.error("Database error checking if artist ID: {} is linked to event ID: {}", artistId, eventId, e);
            throw new ArtistRelationshipException("Failed to check artist-event relationship", e);
        } catch (Exception e) {
            logger.error("Unexpected error checking artist-event relationship", e);
            throw new ArtistRelationshipException("Unexpected error checking artist-event relationship", e);
        }
    }

    /**
     * Inserts multiple event-artist relationships in a single batch operation.
     * <p>
     * This private method handles the low-level details of executing a batch insert
     * for multiple artist-event relationships. Using batch operations significantly
     * improves performance compared to individual inserts, especially for a large
     * number of relationships.
     * <p>
     * The method includes logging and detailed exception handling to help diagnose
     * batch operation issues.
     *
     * @param eventArtistPairs List of event-artist ID pairs to insert
     * @throws ArtistRelationshipException if there's an error during batch insert
     */
    private void batchLinkArtistsToEvent(List<Object[]> eventArtistPairs) {
        if (eventArtistPairs == null || eventArtistPairs.isEmpty()) {
            logger.debug("No relationships to batch insert");
            return;
        }

        try {
            logger.debug("Executing batch insert for {} event-artist relationships", eventArtistPairs.size());

            int[] updateCounts = jdbcTemplate.batchUpdate(
                    "INSERT INTO event_artists (event_id, artist_id) VALUES (?, ?)",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setLong(1, (Long) eventArtistPairs.get(i)[0]);
                            ps.setLong(2, (Long) eventArtistPairs.get(i)[1]);
                        }

                        @Override
                        public int getBatchSize() {
                            return eventArtistPairs.size();
                        }
                    });

            // Validate all inserts were successful
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] != 1) {
                    logger.warn("Insert for eventId:{} artistId:{} affected {} rows instead of 1",
                            eventArtistPairs.get(i)[0], eventArtistPairs.get(i)[1], updateCounts[i]);
                }
            }

            logger.debug("Successfully inserted {} event-artist relationships", eventArtistPairs.size());

        } catch (DataAccessException e) {
            logger.error("Database error during batch insert of event-artist relationships", e);
            throw new ArtistRelationshipException("Failed to insert event-artist relationships", e);
        } catch (Exception e) {
            logger.error("Unexpected error during batch insert of event-artist relationships", e);
            throw new ArtistRelationshipException("Unexpected error inserting event-artist relationships", e);
        }
    }
}