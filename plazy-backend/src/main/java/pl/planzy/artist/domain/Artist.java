package pl.planzy.artist.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.planzy.event.domain.Event;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity class representing an artist in the system.
 * <p>
 * An artist can be associated with multiple events through a many-to-many relationship.
 * This entity is the cornerstone of the artist domain model and is stored in the
 * "artists" table in the database.
 * <p>
 * The class leverages Lombok annotations to reduce boilerplate code for getters,
 * setters, and constructors. It also implements proper equals, hashCode, and toString
 * methods to ensure correct behavior in collections and logging.
 * <p>
 * The artist name has a unique index to prevent duplicate artists in the system.
 *
 * @see Event
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "artists", indexes = {
        @Index(name = "idx_artist_name", columnList = "artist_name", unique = true)
})
public class Artist {

    /**
     * The primary key for the artist entity.
     * Auto-generated using database identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the artist.
     * This field is required and must be unique across all artists.
     */
    @Column(name = "artist_name", nullable = false)
    private String artistName;

    /**
     * The set of events associated with this artist.
     * <p>
     * This represents a many-to-many relationship between artists and events,
     * managed by the "event_artists" join table. The relationship is lazy-loaded
     * to improve performance.
     */
    @ManyToMany(mappedBy = "artists", fetch = FetchType.LAZY)
    private Set<Event> events = new HashSet<>();

    /**
     * Determines if this artist equals another object.
     * <p>
     * Two artists are considered equal if they have the same ID.
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return Objects.equals(id, artist.id);
    }

    /**
     * Generates a hash code for this artist.
     * <p>
     * The hash code is based on the ID if present, or the name otherwise.
     * This ensures entities maintain consistent hash codes before and after
     * being persisted.
     *
     * @return A hash code value for this artist
     */
    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : artistName);
    }

    /**
     * Returns a string representation of this artist.
     * <p>
     * Includes the ID and name for easy identification in logs and debugging.
     *
     * @return A string representation of the artist
     */
    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", artistName='" + artistName + '\'' +
                '}';
    }
}