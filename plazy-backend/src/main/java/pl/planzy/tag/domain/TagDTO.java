package pl.planzy.tag.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for the Tag entity.
 * <p>
 * This class is used to transfer tag data between the service layer and the API
 * without exposing internal implementation details. It contains only the essential
 * properties needed for client applications.
 * <p>
 * TagDTO simplifies data exposure by:
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
public class TagDTO {

    /**
     * The unique identifier of the tag.
     * Matches the ID from the Tag entity.
     */
    private Long id;

    /**
     * The name of the tag.
     * This is the primary display value for the tag.
     */
    private String tagName;
}