package pl.planzy.tag.domain;

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
 * Entity class representing a tag in the system.
 * <p>
 * A tag can be associated with multiple events through a many-to-many relationship.
 * Tags provide a flexible way to categorize and filter events based on their
 * characteristics, themes, or any other classification criteria.
 * <p>
 * This entity is stored in the "tags" table in the database with a unique index on
 * the tag name to prevent duplicates.
 * <p>
 * The class leverages Lombok annotations to reduce boilerplate code for getters,
 * setters, and constructors. It also implements proper equals, hashCode, and toString
 * methods to ensure correct behavior in collections and logging.
 *
 * @see Event
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tag_name", columnList = "tag_name", unique = true)
})
public class Tag {

        /**
         * The primary key for the tag entity.
         * Auto-generated using database identity strategy.
         */
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /**
         * The name of the tag.
         * This field is required and must be unique across all tags.
         * It represents the main identifier for the tag from a user perspective.
         */
        @Column(name = "tag_name", nullable = false)
        private String tagName;

        /**
         * The set of events associated with this tag.
         * <p>
         * This represents a many-to-many relationship between tags and events,
         * managed by the "event_tags" join table. The relationship is lazy-loaded
         * to improve performance.
         */
        @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
        private Set<Event> events = new HashSet<>();

        /**
         * Determines if this tag equals another object.
         * <p>
         * Two tags are considered equal if they have the same ID.
         * This implementation supports proper functioning within collections.
         *
         * @param o The object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Tag tag = (Tag) o;
                return Objects.equals(id, tag.id);
        }

        /**
         * Generates a hash code for this tag.
         * <p>
         * The hash code is based on the ID if present, or the name otherwise.
         * This ensures entities maintain consistent hash codes before and after
         * being persisted.
         *
         * @return A hash code value for this tag
         */
        @Override
        public int hashCode() {
                return Objects.hash(id != null ? id : tagName);
        }

        /**
         * Returns a string representation of this tag.
         * <p>
         * Includes the ID and name for easy identification in logs and debugging.
         *
         * @return A string representation of the tag
         */
        @Override
        public String toString() {
                return "Tag{" +
                        "id=" + id +
                        ", tagName='" + tagName + '\'' +
                        '}';
        }
}