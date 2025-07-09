package pl.planzy.event.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByUrl(String url);

    @Query(value = "SELECT e.* FROM events e WHERE e.embedding IS NULL LIMIT :limit",
            nativeQuery = true)
    List<Event> findEventsWithoutEmbeddingsNative(@Param("limit") int limit);

    /**
     * Count events without embeddings
     * @return Count of events without embeddings
     */
    @Query(value = "SELECT COUNT(*) FROM events WHERE embedding IS NULL",
            nativeQuery = true)
    int countEventsWithoutEmbeddings();

    /**
     * Find future events with places using native query to avoid PostgreSQL DISTINCT+ORDER BY limitation
     * Modified to use consistent snake_case column naming in ORDER BY clause
     */
    @Query(value = "SELECT e.id FROM events e " +
            "WHERE e.start_date >= :now " +
            "AND e.place_id IS NOT NULL " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:location IS NULL OR e.location = :location) " +
            "AND (:artist IS NULL OR EXISTS (SELECT 1 FROM event_artists ea JOIN artists a ON ea.artist_id = a.id " +
            "WHERE a.artist_name = :artist AND e.id = ea.event_id)) " +
            "AND (:tag IS NULL OR EXISTS (SELECT 1 FROM event_tags et JOIN tags t ON et.tag_id = t.id " +
            "WHERE t.tag_name = :tag AND e.id = et.event_id)) " +
            "ORDER BY e.start_date",
            countQuery = "SELECT COUNT(*) FROM events e " +
                    "WHERE e.start_date >= :now " +
                    "AND e.place_id IS NOT NULL " +
                    "AND (:category IS NULL OR e.category = :category) " +
                    "AND (:location IS NULL OR e.location = :location) " +
                    "AND (:artist IS NULL OR EXISTS (SELECT 1 FROM event_artists ea JOIN artists a ON ea.artist_id = a.id " +
                    "WHERE a.artist_name = :artist AND e.id = ea.event_id)) " +
                    "AND (:tag IS NULL OR EXISTS (SELECT 1 FROM event_tags et JOIN tags t ON et.tag_id = t.id " +
                    "WHERE t.tag_name = :tag AND e.id = et.event_id))",
            nativeQuery = true)
    Page<Long> findEventIdsByFilters(
            @Param("now") LocalDateTime now,
            @Param("category") String category,
            @Param("location") String location,
            @Param("artist") String artist,
            @Param("tag") String tag,
            Pageable pageable);

    /**
     * Load events with their relationships by ID list
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.place " +
            "LEFT JOIN FETCH e.artists " +
            "LEFT JOIN FETCH e.tags " +
            "WHERE e.id IN :ids")
    List<Event> findByIdInWithRelationships(@Param("ids") List<Long> ids);
}