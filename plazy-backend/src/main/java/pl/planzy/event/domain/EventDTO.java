package pl.planzy.event.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.planzy.artist.domain.ArtistDTO;
import pl.planzy.place.domain.PlaceDTO;
import pl.planzy.tag.domain.TagDTO;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Data Transfer Object for Event entity.
 * Used to transfer event data between the API and clients.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {

    private Long id;
    private String eventName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    private String thumbnail;
    private String url;
    private String location;
    private String category;
    private String description;
    private String source;

    // Related data
    private PlaceDTO place;
    private Set<ArtistDTO> artists = new HashSet<>();
    private Set<TagDTO> tags = new HashSet<>();

    // Additional computed properties
    private boolean isFeatured;
    private double popularityScore;
    private int daysUntilEvent;
}