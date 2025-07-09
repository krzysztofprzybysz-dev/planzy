package pl.planzy.place.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Place entity.
 * <p>
 * This class is used to transfer place data between the service layer and the API
 * without exposing internal implementation details. It contains only the essential
 * properties needed for client applications.
 * <p>
 * PlaceDTO simplifies data exposure by:
 * <ul>
 *   <li>Excluding bidirectional relationships that could cause circular references</li>
 *   <li>Preventing lazy-loading issues when entity objects leave the persistence context</li>
 *   <li>Providing a flat, simplified representation optimized for API responses</li>
 * </ul>
 * <p>
 * Lombok's {@code @Data} annotation automatically generates getters, setters,
 * equals, hashCode, and toString methods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {

    /**
     * The unique identifier of the place (Google Places ID).
     */
    private String id;

    /**
     * The name of the place.
     * This is the primary display value for the place.
     */
    private String name;

    /**
     * The complete formatted address of the place.
     */
    private String formattedAddress;

    /**
     * The latitude coordinate of the place location.
     */
    private Double latitude;

    /**
     * The longitude coordinate of the place location.
     */
    private Double longitude;

    /**
     * The city where the place is located.
     */
    private String city;

    /**
     * The country where the place is located.
     */
    private String country;

    /**
     * The official website URL of the place.
     */
    private String website;

    /**
     * The contact phone number of the place.
     */
    private String phoneNumber;

    /**
     * The average rating of the place (typically on a 1-5 scale).
     */
    private Double rating;

    /**
     * The total number of user ratings contributing to the rating.
     */
    private Integer userRatingsTotal;

    /**
     * A calculated popularity score based on rating and review count.
     */
    private Double popularityScore;

    /**
     * The price level of the place, typically on a 0-4 scale.
     */
    private Integer priceLevel;

    /**
     * Comma-separated list of place types or categories.
     */
    private String placeTypes;

    /**
     * Reference to the primary photo of the place in Google's system.
     */
    private String photoReference;
}