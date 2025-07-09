package pl.planzy.event.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import pl.planzy.artist.domain.Artist;
import pl.planzy.place.domain.Place;
import pl.planzy.tag.domain.Tag;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 500)
    private String eventName;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "thumbnail", nullable = false, length = 500)
    private String thumbnail;

    @Column(name = "url", nullable = false, length = 500, unique = true)
    private String url;

    @Column(name = "location", nullable = false, length = 500)
    private String location;

    @Column(name = "category", nullable = false, length = 500)
    private String category;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "source", nullable = false, length = 500)
    private String source;

    @Column(name = "embedding", columnDefinition = "vector(1536)", nullable = true, insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
// Add this annotation to tell Hibernate to never include this field in queries
    @Formula("null")
    private byte[] embedding;

    // Relationship mappings
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    // Simplified method that avoids bidirectional relationship issues
    public void setPlace(Place place) {
        this.place = place;
    }

    /**
     * Add an artist to this event and handle the bidirectional relationship.
     * This method ensures proper entity state management.
     */
    public boolean addArtist(Artist artist) {
        if (artist != null && !this.artists.contains(artist)) {
            this.artists.add(artist);
            artist.getEvents().add(this);
            return true;
        }
        return false;
    }

    /**
     * Add a tag to this event and handle the bidirectional relationship.
     * This method ensures proper entity state management.
     */
    public boolean addTag(Tag tag) {
        if (tag != null && !this.tags.contains(tag)) {
            this.tags.add(tag);
            tag.getEvents().add(this);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id) ||
                (id == null && event.id == null && Objects.equals(url, event.url));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : url);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", event_name='" + eventName + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}