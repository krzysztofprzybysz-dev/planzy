package pl.planzy.place.service;

import org.springframework.stereotype.Component;
import pl.planzy.place.domain.Place;
import pl.planzy.place.domain.PlaceDTO;

/**
 * Mapper for converting between Place entities and DTOs.
 */
@Component
public class PlaceMapper {

    /**
     * Convert a Place entity to a PlaceDTO
     */
    public PlaceDTO toDTO(Place place) {
        if (place == null) {
            return null;
        }

        PlaceDTO dto = new PlaceDTO();

        // Set basic properties
        dto.setId(place.getGooglePlaceId());
        dto.setName(place.getNameGoogle() != null ? place.getNameGoogle() : place.getNameScrapped());
        dto.setFormattedAddress(place.getFormattedAddress());
        dto.setLatitude(place.getLatitude());
        dto.setLongitude(place.getLongitude());
        dto.setCity(place.getCity());
        dto.setCountry(place.getCountry());
        dto.setWebsite(place.getWebsite());
        dto.setPhoneNumber(place.getPhoneNumber());
        dto.setRating(place.getRating());
        dto.setUserRatingsTotal(place.getUserRatingsTotal());
        dto.setPopularityScore(place.getPopularityScore());
        dto.setPriceLevel(place.getPriceLevel());
        dto.setPlaceTypes(place.getPlaceTypes());
        dto.setPhotoReference(place.getPrimaryPhotoReference());

        return dto;
    }

    /**
     * Convert a PlaceDTO to a Place entity
     */
    public Place toEntity(PlaceDTO dto) {
        if (dto == null) {
            return null;
        }

        Place place = new Place();

        place.setGooglePlaceId(dto.getId());
        place.setNameGoogle(dto.getName());
        place.setFormattedAddress(dto.getFormattedAddress());
        place.setLatitude(dto.getLatitude());
        place.setLongitude(dto.getLongitude());
        place.setCity(dto.getCity());
        place.setCountry(dto.getCountry());
        place.setWebsite(dto.getWebsite());
        place.setPhoneNumber(dto.getPhoneNumber());
        place.setRating(dto.getRating());
        place.setUserRatingsTotal(dto.getUserRatingsTotal());
        place.setPopularityScore(dto.getPopularityScore());
        place.setPriceLevel(dto.getPriceLevel());
        place.setPlaceTypes(dto.getPlaceTypes());
        place.setPrimaryPhotoReference(dto.getPhotoReference());

        return place;
    }
}