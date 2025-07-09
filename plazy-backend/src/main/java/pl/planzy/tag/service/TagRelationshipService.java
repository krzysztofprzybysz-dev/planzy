package pl.planzy.tag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.planzy.tag.exception.TagRelationshipException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing relationships between tags and events.
 * <p>
 * This service provides specialized methods for creating and managing the
 * many-to-many relationship between tags and events. It uses optimized
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
public class TagRelationshipService {
    private static final Logger logger = LoggerFactory.getLogger(TagRelationshipService.class);

    /**
     * JDBC template for database access.
     * Provides methods for executing SQL queries and updates,
     * particularly batch operations.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs a new TagRelationshipService with the required dependencies.
     *
     * @param jdbcTemplate JDBC template for database access
     */
    @Autowired
    public TagRelationshipService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates relationships between an event and multiple tags.
     * <p>
     * This method efficiently links multiple tags to an event in a single
     * transaction, using batch operations for optimal performance. It checks for
     * existing relationships to avoid duplicates and handle the operation
     * idempotently.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation and facilitate troubleshooting in production environments.
     *
     * @param eventId The ID of the event
     * @param tagIds List of tag IDs to relate to the event
     * @throws TagRelationshipException if there's an error creating the relationships
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void linkTagsToEvent(Long eventId, List<Long> tagIds) {
        if (eventId == null) {
            logger.warn("Attempted to link tags to null event ID");
            return;
        }

        if (tagIds == null || tagIds.isEmpty()) {
            logger.debug("No tags to link to event ID: {}", eventId);
            return;
        }

        logger.info("Linking {} tags to event ID: {}", tagIds.size(), eventId);

        try {
            List<Object[]> eventTagPairs = new ArrayList<>();
            int skippedCount = 0;

            // First check which relationships already exist to avoid duplicates
            for (Long tagId : tagIds) {
                if (tagId == null) {
                    logger.warn("Null tag ID found in link request for event ID: {}", eventId);
                    skippedCount++;
                    continue;
                }

                if (isTagLinkedToEvent(eventId, tagId)) {
                    logger.debug("Tag ID: {} already linked to event ID: {}, skipping", tagId, eventId);
                    skippedCount++;
                    continue;
                }

                eventTagPairs.add(new Object[]{eventId, tagId});
            }

            if (skippedCount > 0) {
                logger.info("Skipped {} tags already linked to event ID: {}", skippedCount, eventId);
            }

            if (eventTagPairs.isEmpty()) {
                logger.info("No new tag relationships to create for event ID: {}", eventId);
                return;
            }

            // Use batch operation for better performance with multiple relationships
            batchLinkTagsToEvent(eventTagPairs);
            logger.info("Successfully linked {} tags to event ID: {}", eventTagPairs.size(), eventId);

        } catch (TagRelationshipException e) {
            // Just re-throw if it's already our custom exception
            throw e;
        } catch (Exception e) {
            logger.error("Error linking tags to event ID: {}", eventId, e);
            throw new TagRelationshipException("Failed to link tags to event", e);
        }
    }

    /**
     * Checks if a relationship exists between an event and a tag.
     * <p>
     * This method verifies whether a specific tag is already linked to a given
     * event. It's used both as a standalone method and internally to avoid creating
     * duplicate relationships.
     * <p>
     * The method includes validation, logging, and exception handling to ensure
     * reliable operation in production environments.
     *
     * @param eventId The ID of the event
     * @param tagId The ID of the tag
     * @return true if the relationship exists, false otherwise
     * @throws TagRelationshipException if there's an error checking the relationship
     */
    public boolean isTagLinkedToEvent(Long eventId, Long tagId) {
        if (eventId == null || tagId == null) {
            logger.warn("Attempted to check relationship with null eventId: {} or tagId: {}", eventId, tagId);
            return false;
        }

        try {
            logger.debug("Checking if tag ID: {} is linked to event ID: {}", tagId, eventId);

            // Using COUNT instead of EXISTS for better compatibility across databases
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM event_tags WHERE event_id = ? AND tag_id = ?",
                    Integer.class, eventId, tagId);

            boolean isLinked = count != null && count > 0;
            logger.debug("Tag ID: {} is {} linked to event ID: {}", tagId, isLinked ? "already" : "not", eventId);

            return isLinked;

        } catch (DataAccessException e) {
            logger.error("Database error checking if tag ID: {} is linked to event ID: {}", tagId, eventId, e);
            throw new TagRelationshipException("Failed to check tag-event relationship", e);
        } catch (Exception e) {
            logger.error("Unexpected error checking tag-event relationship", e);
            throw new TagRelationshipException("Unexpected error checking tag-event relationship", e);
        }
    }

    /**
     * Inserts multiple event-tag relationships in a single batch operation.
     * <p>
     * This private method handles the low-level details of executing a batch insert
     * for multiple tag-event relationships. Using batch operations significantly
     * improves performance compared to individual inserts, especially for a large
     * number of relationships.
     * <p>
     * The method includes logging and detailed exception handling to help diagnose
     * batch operation issues.
     *
     * @param eventTagPairs List of event-tag ID pairs to insert
     * @throws TagRelationshipException if there's an error during batch insert
     */
    private void batchLinkTagsToEvent(List<Object[]> eventTagPairs) {
        if (eventTagPairs == null || eventTagPairs.isEmpty()) {
            logger.debug("No relationships to batch insert");
            return;
        }

        try {
            logger.debug("Executing batch insert for {} event-tag relationships", eventTagPairs.size());

            int[] updateCounts = jdbcTemplate.batchUpdate(
                    "INSERT INTO event_tags (event_id, tag_id) VALUES (?, ?)",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setLong(1, (Long) eventTagPairs.get(i)[0]);
                            ps.setLong(2, (Long) eventTagPairs.get(i)[1]);
                        }

                        @Override
                        public int getBatchSize() {
                            return eventTagPairs.size();
                        }
                    });

            // Validate all inserts were successful
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] != 1) {
                    logger.warn("Insert for eventId:{} tagId:{} affected {} rows instead of 1",
                            eventTagPairs.get(i)[0], eventTagPairs.get(i)[1], updateCounts[i]);
                }
            }

            logger.debug("Successfully inserted {} event-tag relationships", eventTagPairs.size());

        } catch (DataAccessException e) {
            logger.error("Database error during batch insert of event-tag relationships", e);
            throw new TagRelationshipException("Failed to insert event-tag relationships", e);
        } catch (Exception e) {
            logger.error("Unexpected error during batch insert of event-tag relationships", e);
            throw new TagRelationshipException("Unexpected error inserting event-tag relationships", e);
        }
    }
}