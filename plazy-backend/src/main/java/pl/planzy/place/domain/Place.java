package pl.planzy.place.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.planzy.event.domain.Event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity class representing a place in the system.
 * <p>
 * This entity stores information about venues where events occur, including
 * basic details and enriched data from the Google Places API. The Place entity
 * is a cornerstone of location-based functionality in the system.
 * <p>
 * Places can be associated with multiple events through a one-to-many
 * relationship, where each event references a specific place.
 * <p>
 * The primary identifier for this entity is the Google Place ID, which
 * provides a stable, globally unique identifier for each location.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "places")
public class Place {

    /**
     * The primary key for the place entity.
     * Uses the Google Place ID as a natural identifier.
     */
    @Id
    @Column(name = "place_id")
    private String googlePlaceId;

    /**
     * The original name of the place as collected from scraping systems.
     * This helps maintain the connection to the original data source.
     */
    @Column(name = "place_name_scrapped")
    private String nameScrapped;

    /**
     * The official name of the place as provided by Google Places API.
     * This is typically more standardized and recognized than the scraped name.
     */
    @Column(name = "place_name_google")
    private String nameGoogle;

    /**
     * The complete formatted address of the place.
     * Provided by Google Places API in a standardized format.
     */
    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    /**
     * The latitude coordinate of the place location.
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * The longitude coordinate of the place location.
     */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * The street number component of the address.
     */
    @Column(name = "street_number")
    private String streetNumber;

    /**
     * The street name component of the address.
     */
    @Column(name = "street")
    private String street;

    /**
     * The neighborhood or district where the place is located.
     */
    @Column(name = "neighborhood")
    private String neighborhood;

    /**
     * The city where the place is located.
     */
    @Column(name = "city")
    private String city;

    /**
     * The state or province where the place is located.
     */
    @Column(name = "state")
    private String state;

    /**
     * The country where the place is located.
     */
    @Column(name = "country")
    private String country;

    /**
     * The postal code or ZIP code of the place address.
     */
    @Column(name = "postal_code")
    private String postalCode;

    /**
     * The official website URL of the place.
     */
    @Column(name = "website", length = 500)
    private String website;

    /**
     * The contact phone number of the place.
     */
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * The average rating of the place (typically on a 1-5 scale).
     * Sourced from Google Places API based on user reviews.
     */
    @Column(name = "rating")
    private Double rating;

    /**
     * The total number of user ratings contributing to the rating.
     * Important for determining rating confidence.
     */
    @Column(name = "user_ratings_total")
    private Integer userRatingsTotal;

    /**
     * A calculated popularity score based on rating and review count.
     * Used for ranking and recommendations.
     */
    @Column(name = "popularity_score")
    private Double popularityScore;

    /**
     * The price level of the place, typically on a 0-4 scale.
     * 0 = Free, 1 = Inexpensive, 2 = Moderate, 3 = Expensive, 4 = Very Expensive.
     */
    @Column(name = "price_level")
    private Integer priceLevel;

    /**
     * Comma-separated list of place types or categories.
     * Used for filtering and classification.
     */
    @Column(name = "place_types", length = 500)
    private String placeTypes;

    /**
     * Reference to the primary photo of the place in Google's system.
     * Can be used to retrieve the actual image through the Photos API.
     */
    @Column(name = "primary_photo_reference", length = 2000)
    private String primaryPhotoReference;

    /**
     * The number of reviews available for this place.
     */
    @Column(name = "review_count")
    private Integer reviewCount;

    /**
     * Timestamp of when the place data was last enriched from external sources.
     * Used to determine when data needs refreshing.
     */
    @Column(name = "last_enriched_date")
    private LocalDateTime lastEnrichedDate;

    /**
     * The list of events associated with this place.
     * Represents a one-to-many relationship from place to events.
     */
    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

    /**
     * Determines if this place equals another object.
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Objects.equals(googlePlaceId, place.googlePlaceId);
    }

    /**
     * Generates a hash code for this place.
     *
     * @return A hash code value for this place
     */
    @Override
    public int hashCode() {
        return Objects.hash(googlePlaceId);
    }

    /**
     * Returns a string representation of this place.
     *
     * @return A string representation of the place
     */
    @Override
    public String toString() {
        return "Place{" +
                "googlePlaceId='" + googlePlaceId + '\'' +
                ", nameScrapped='" + nameScrapped + '\'' +
                ", nameGoogle='" + nameGoogle + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}