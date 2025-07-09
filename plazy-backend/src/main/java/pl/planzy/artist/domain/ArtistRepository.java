package pl.planzy.artist.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Artist entities.
 * <p>
 * This interface provides standard CRUD operations for Artist entities,
 * leveraging Spring Data JPA to automatically generate the implementation
 * at runtime. It extends JpaRepository to inherit common methods like:
 * <ul>
 *   <li>save() - Create or update an artist</li>
 *   <li>findById() - Find an artist by ID</li>
 *   <li>findAll() - Retrieve all artists</li>
 *   <li>delete() - Delete an artist</li>
 *   <li>count() - Count the number of artists</li>
 * </ul>
 * <p>
 * The repository serves as the data access layer for artists, abstracting
 * away the details of database interactions. It works with both Artist entities
 * and their Long ID type.
 * <p>
 * This interface can be extended with custom query methods as needed, either
 * using Spring Data JPA's method name conventions or custom JPQL/SQL queries
 * via annotations.
 */

public interface ArtistRepository extends JpaRepository<Artist, Long> {


    /**
     * Finds an artist by name.
     */
    Optional<Artist> findByArtistName(String artistName);



    /**
     * Searches for artists whose names contain the given text (case insensitive).
     */
    @Query("SELECT a FROM Artist a WHERE LOWER(a.artistName) LIKE LOWER(CONCAT('%', :nameFragment, '%'))")
    List<Artist> searchArtistsByName(@Param("nameFragment") String nameFragment);


    /**
     * Finds all artists associated with a specific event.
     */
    @Query("SELECT a FROM Artist a JOIN a.events e WHERE e.id = :eventId")
    List<Artist> findByEventId(@Param("eventId") Long eventId);
}