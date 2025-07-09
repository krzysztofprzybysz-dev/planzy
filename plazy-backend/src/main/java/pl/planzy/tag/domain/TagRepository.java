package pl.planzy.tag.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Tag entities.
 * <p>
 * This interface provides standard CRUD operations for Tag entities,
 * leveraging Spring Data JPA to automatically generate the implementation
 * at runtime. It extends JpaRepository to inherit common methods like:
 * <ul>
 *   <li>save() - Create or update a tag</li>
 *   <li>findById() - Find a tag by ID</li>
 *   <li>findAll() - Retrieve all tags</li>
 *   <li>delete() - Delete a tag</li>
 *   <li>count() - Count the number of tags</li>
 * </ul>
 * <p>
 * The repository serves as the data access layer for tags, abstracting
 * away the details of database interactions. It works with both Tag entities
 * and their Long ID type.
 * <p>
 * This interface includes custom query methods for specialized tag lookups
 * beyond the standard CRUD operations.
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Finds a tag by its exact name.
     * <p>
     * This is useful for ensuring tag uniqueness and lookups by natural key.
     *
     * @param tagName The exact tag name to search for
     * @return An Optional containing the tag if found, or empty if not found
     */
    Optional<Tag> findByTagName(String tagName);

    /**
     * Searches for tags whose names contain the given text (case insensitive).
     * <p>
     * This method is useful for implementing search functionality.
     *
     * @param nameFragment The text to search for within tag names
     * @return A list of matching tags, or an empty list if none found
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :nameFragment, '%'))")
    List<Tag> searchTagsByName(@Param("nameFragment") String nameFragment);

    /**
     * Finds all tags associated with a specific event.
     * <p>
     * This query uses a join to efficiently retrieve all tags
     * attached to the given event.
     *
     * @param eventId The ID of the event
     * @return A list of tags associated with the event
     */
    @Query("SELECT t FROM Tag t JOIN t.events e WHERE e.id = :eventId")
    List<Tag> findByEventId(@Param("eventId") Long eventId);
}