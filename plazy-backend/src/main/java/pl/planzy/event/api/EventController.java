package pl.planzy.event.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.planzy.event.domain.EventDTO;
import pl.planzy.embedding.service.EmbeddingService;
import pl.planzy.event.service.EventService;

import java.util.List;

/**
 * REST controller for event-related operations.
 * Provides endpoints for browsing, searching, and retrieving event data.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;
    private final EmbeddingService embeddingService;

    @Autowired
    public EventController(EventService eventService, EmbeddingService embeddingService) {
        this.eventService = eventService;
        this.embeddingService = embeddingService;
    }

    /**
     * Get paginated list of events with optional filtering
     */
    @GetMapping
    public ResponseEntity<Page<EventDTO>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "start_date") String sort, // Changed from "startDate" to "start_date"
            @RequestParam(defaultValue = "asc") String direction
    ) {
        // Create pageable request
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // Get events with filters
        Page<EventDTO> events = eventService.getEvents(pageable, category, location, artist, tag);

        return ResponseEntity.ok(events);
    }

    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEvent(@PathVariable Long id) {
        EventDTO event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }

    /**
     * Basic text search on events
     */
    @GetMapping("/search")
    public ResponseEntity<List<EventDTO>> searchEvents(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<EventDTO> results = eventService.searchEvents(query, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Use vector embeddings to find semantically similar events
     */
    @GetMapping("/similar")
    public ResponseEntity<List<EventDTO>> getSimilarEvents(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<EventDTO> similarEvents = eventService.findSimilarEvents(query, limit);
        return ResponseEntity.ok(similarEvents);
    }

    /**
     * Get upcoming events (next 7 days)
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventDTO>> getUpcomingEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<EventDTO> upcomingEvents = eventService.getUpcomingEvents(limit);
        return ResponseEntity.ok(upcomingEvents);
    }

    /**
     * Get featured or popular events
     */
    @GetMapping("/featured")
    public ResponseEntity<List<EventDTO>> getFeaturedEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<EventDTO> featuredEvents = eventService.getFeaturedEvents(limit);
        return ResponseEntity.ok(featuredEvents);
    }

    /**
     * Get available categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = eventService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get available locations
     */
    @GetMapping("/locations")
    public ResponseEntity<List<String>> getLocations() {
        List<String> locations = eventService.getAllLocations();
        return ResponseEntity.ok(locations);
    }
}