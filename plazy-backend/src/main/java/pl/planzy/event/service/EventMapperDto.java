package pl.planzy.event.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.planzy.artist.domain.Artist;
import pl.planzy.artist.domain.ArtistDTO;
import pl.planzy.place.service.PlaceMapper;
import pl.planzy.tag.domain.Tag;
import pl.planzy.tag.domain.TagDTO;
import pl.planzy.event.domain.Event;
import pl.planzy.event.domain.EventDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Event entities and DTOs.
 */
@Component
public class EventMapperDto {

    private final PlaceMapper placeMapper;

    @Autowired
    public EventMapperDto(PlaceMapper placeMapper) {
        this.placeMapper = placeMapper;
    }

    /**
     * Convert an Event entity to an EventDTO
     */
    public EventDTO toDTO(Event event) {
        if (event == null) {
            return null;
        }

        EventDTO dto = new EventDTO();

        // Basic properties
        dto.setId(event.getId());
        dto.setEventName(event.getEventName());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setThumbnail(event.getThumbnail());
        dto.setUrl(event.getUrl());
        dto.setLocation(event.getLocation());
        dto.setCategory(event.getCategory());
        dto.setDescription(event.getDescription());
        dto.setSource(event.getSource());

        // Place
        if (event.getPlace() != null) {
            dto.setPlace(placeMapper.toDTO(event.getPlace()));

            // Set popularity score from place if available
            if (event.getPlace().getPopularityScore() != null) {
                dto.setPopularityScore(event.getPlace().getPopularityScore());
                // Consider an event "featured" if its venue has a high popularity score
                dto.setFeatured(event.getPlace().getPopularityScore() > 85.0);
            }
        }

        // Artists
        if (event.getArtists() != null && !event.getArtists().isEmpty()) {
            dto.setArtists(event.getArtists().stream()
                    .map(this::toArtistDTO)
                    .collect(Collectors.toSet()));
        }

        // Tags
        if (event.getTags() != null && !event.getTags().isEmpty()) {
            dto.setTags(event.getTags().stream()
                    .map(this::toTagDTO)
                    .collect(Collectors.toSet()));
        }

        // Calculate days until event
        if (event.getStartDate() != null && event.getStartDate().isAfter(LocalDateTime.now())) {
            dto.setDaysUntilEvent((int) ChronoUnit.DAYS.between(LocalDate.now(), event.getStartDate().toLocalDate()));
        } else {
            dto.setDaysUntilEvent(0);
        }

        return dto;
    }

    /**
     * Convert an EventDTO to an Event entity
     */
    public Event toEntity(EventDTO dto) {
        if (dto == null) {
            return null;
        }

        Event event = new Event();

        // Only set the ID if it's an existing entity
        if (dto.getId() != null) {
            event.setId(dto.getId());
        }

        event.setEventName(dto.getEventName());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setThumbnail(dto.getThumbnail());
        event.setUrl(dto.getUrl());
        event.setLocation(dto.getLocation());
        event.setCategory(dto.getCategory());
        event.setDescription(dto.getDescription());
        event.setSource(dto.getSource());

        return event;
    }

    /**
     * Convert an Artist entity to an ArtistDTO
     */
    private ArtistDTO toArtistDTO(Artist artist) {
        if (artist == null) {
            return null;
        }

        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        dto.setArtistName(artist.getArtistName());

        return dto;
    }

    /**
     * Convert a Tag entity to a TagDTO
     */
    private TagDTO toTagDTO(Tag tag) {
        if (tag == null) {
            return null;
        }

        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setTagName(tag.getTagName());

        return dto;
    }
}