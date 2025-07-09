package pl.planzy.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.planzy.event.domain.Event;
import pl.planzy.event.domain.EventDTO;
import pl.planzy.event.domain.EventRepository;
import pl.planzy.embedding.service.EmbeddingService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for retrieving and managing event data.
 * Provides methods for fetching, searching, and filtering events.
 */
@Service
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EventMapperDto eventMapperDto;
    private final EmbeddingService embeddingService;

    @Autowired
    public EventService(
            EventRepository eventRepository,
            JdbcTemplate jdbcTemplate,
            EventMapperDto eventMapperDto,
            EmbeddingService embeddingService) {
        this.eventRepository = eventRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.eventMapperDto = eventMapperDto;
        this.embeddingService = embeddingService;
    }

    /**
     * Get paginated events with optional filtering.
     * Only returns future events that have a place.
     */
    @Transactional(readOnly = true)
    public Page<EventDTO> getEvents(Pageable pageable, String category, String location, String artist, String tag) {
        LocalDateTime now = LocalDateTime.now();

        logger.info("Fetching events with filters: category={}, location={}, artist={}, tag={}",
                category, location, artist, tag);

        // First, run a diagnostic query to check event counts
        long totalEvents = eventRepository.count();
        long futureEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE start_date >= ?",
                Long.class, now);
        long eventsWithPlaces = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE place_id IS NOT NULL",
                Long.class);
        long futureEventsWithPlaces = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE start_date >= ? AND place_id IS NOT NULL",
                Long.class, now);

        logger.info("Event counts: Total={}, Future={}, WithPlaces={}, FutureWithPlaces={}",
                totalEvents, futureEvents, eventsWithPlaces, futureEventsWithPlaces);

        // Step 1: Get paginated event IDs with filters (using native query to avoid PostgreSQL DISTINCT+ORDER BY issue)
        Page<Long> eventIdPage = eventRepository.findEventIdsByFilters(
                now, category, location, artist, tag, pageable);

        if (eventIdPage.isEmpty()) {
            logger.info("No events found matching the criteria");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        logger.info("Found {} events matching the criteria", eventIdPage.getTotalElements());

        // Step 2: Fetch full events with relationships
        List<Event> events = eventRepository.findByIdInWithRelationships(eventIdPage.getContent());

        // Step 3: Sort events to maintain pagination order from the initial query
        // We no longer need to sort manually as the native query already handles sorting

        // Step 4: Map to DTOs
        List<EventDTO> dtos = events.stream()
                .map(eventMapperDto::toDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, eventIdPage.getTotalElements());
    }

    /**
     * Helper to preserve ID order from the paginated query
     */
    private List<Event> sortEventsByPage(List<Event> events, List<Long> orderedIds) {
        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        return orderedIds.stream()
                .map(eventMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public EventDTO getEventById(Long id) {
        Optional<Event> event = eventRepository.findById(id);
        return event.map(eventMapperDto::toDTO).orElse(null);
    }

    /**
     * Basic text search on events
     */
    @Transactional(readOnly = true)
    public List<EventDTO> searchEvents(String query, int limit) {
        // Update search to include our filters for future events with places
        LocalDateTime now = LocalDateTime.now();

        String sql = "SELECT e.* FROM events e " +
                "WHERE e.start_date >= ? " +  // Only future events
                "AND e.place_id IS NOT NULL " + // Only events with places
                "AND (e.event_name ILIKE ? " +
                "OR e.description ILIKE ? " +
                "OR e.category ILIKE ? " +
                "OR e.location ILIKE ?) " +
                "LIMIT ?";

        String likePattern = "%" + query + "%";

        List<Event> events = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Event event = new Event();
                    event.setId(rs.getLong("id"));
                    event.setEventName(rs.getString("event_name"));
                    event.setDescription(rs.getString("description"));
                    event.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                    event.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                    event.setUrl(rs.getString("url"));
                    event.setThumbnail(rs.getString("thumbnail"));
                    event.setLocation(rs.getString("location"));
                    event.setCategory(rs.getString("category"));
                    event.setSource(rs.getString("source"));
                    return event;
                },
                now, likePattern, likePattern, likePattern, likePattern, limit
        );

        return events.stream()
                .map(eventMapperDto::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Find semantically similar events using vector embeddings
     */
    @Transactional(readOnly = true)
    public List<EventDTO> findSimilarEvents(String query, int limit) {
        // We'll filter the returned events to enforce the same rules
        List<Event> similarEvents = embeddingService.findSimilarEvents(query, limit);

        // Apply our filters
        LocalDateTime now = LocalDateTime.now();
        List<Event> filteredEvents = similarEvents.stream()
                .filter(e -> e.getStartDate() != null && e.getStartDate().isAfter(now))
                .filter(e -> e.getPlace() != null)
                .collect(Collectors.toList());

        return filteredEvents.stream()
                .map(eventMapperDto::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming events (next 7 days)
     */
    @Transactional(readOnly = true)
    public List<EventDTO> getUpcomingEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);

        String sql = "SELECT e.* FROM events e " +
                "WHERE e.start_date >= ? AND e.start_date <= ? " +
                "AND e.place_id IS NOT NULL " + // Only events with places
                "ORDER BY e.start_date ASC " +
                "LIMIT ?";

        List<Event> events = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Event event = new Event();
                    event.setId(rs.getLong("id"));
                    event.setEventName(rs.getString("event_name"));
                    event.setDescription(rs.getString("description"));
                    event.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                    event.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                    event.setUrl(rs.getString("url"));
                    event.setThumbnail(rs.getString("thumbnail"));
                    event.setLocation(rs.getString("location"));
                    event.setCategory(rs.getString("category"));
                    event.setSource(rs.getString("source"));
                    return event;
                },
                now, sevenDaysLater, limit
        );

        return events.stream()
                .map(eventMapperDto::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get featured or popular events
     */
    @Transactional(readOnly = true)
    public List<EventDTO> getFeaturedEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();

        String sql = "SELECT e.* FROM events e " +
                "JOIN places p ON e.place_id = p.place_id " +
                "WHERE e.start_date >= ? " + // Only future events
                "AND p.popularity_score IS NOT NULL " +
                "ORDER BY p.popularity_score DESC, e.start_date ASC " +
                "LIMIT ?";

        List<Event> events = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Event event = new Event();
                    event.setId(rs.getLong("id"));
                    event.setEventName(rs.getString("event_name"));
                    event.setDescription(rs.getString("description"));
                    event.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                    event.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                    event.setUrl(rs.getString("url"));
                    event.setThumbnail(rs.getString("thumbnail"));
                    event.setLocation(rs.getString("location"));
                    event.setCategory(rs.getString("category"));
                    event.setSource(rs.getString("source"));
                    return event;
                },
                now, limit
        );

        return events.stream()
                .map(eventMapperDto::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all available categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT category FROM events WHERE start_date >= ? AND place_id IS NOT NULL ORDER BY category",
                String.class, now);
    }

    /**
     * Get all available locations
     */
    @Transactional(readOnly = true)
    public List<String> getAllLocations() {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT location FROM events WHERE start_date >= ? AND place_id IS NOT NULL ORDER BY location",
                String.class, now);
    }
}