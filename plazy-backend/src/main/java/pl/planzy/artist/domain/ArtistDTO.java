package pl.planzy.artist.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for the Artist entity.
 * <p>
 * This class is used to transfer artist data between the service layer and the API
 * without exposing internal implementation details. It contains only the essential
 * properties needed for client applications.
 * <p>
 * ArtistDTO simplifies data exposure by:
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
public class ArtistDTO {

    /**
     * The unique identifier of the artist.
     * Matches the ID from the Artist entity.
     */
    private Long id;

    /**
     * The name of the artist.
     * This is the primary display value for the artist.
     */
    private String artistName;
}