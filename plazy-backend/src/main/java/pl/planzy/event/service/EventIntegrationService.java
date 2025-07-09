package pl.planzy.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.planzy.artist.domain.Artist;
import pl.planzy.artist.service.ArtistRelationshipService;
import pl.planzy.event.domain.Event;
import pl.planzy.event.domain.EventRepository;
import pl.planzy.tag.domain.Tag;
import pl.planzy.artist.service.ArtistService;
import pl.planzy.place.service.PlaceIntegrationService;
import pl.planzy.tag.service.TagRelationshipService;
import pl.planzy.tag.service.TagService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Comprehensive service for integrating events from external sources into the system.
 * Handles the entire process from raw data to domain entities with relationships.
 * Implements batch processing to handle large numbers of records efficiently.
 */
@Service
public class EventIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(EventIntegrationService.class);

    // Batch processing configuration
    private static final int BATCH_SIZE = 50;

    @PersistenceContext
    private EntityManager entityManager;

    // Core repositories and services
    private final EventRepository eventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ArtistService artistService;
    private final TagService tagService;
    private final PlaceIntegrationService placeIntegrationService;
    private final ArtistRelationshipService artistRelationshipService;
    private final TagRelationshipService tagRelationshipService;

    // Cache for processed URLs to avoid duplicates
    private final Set<String> processedUrls = ConcurrentHashMap.newKeySet();

    // State tracking for batch processing
    private final AtomicInteger currentOffset = new AtomicInteger(0);
    private final AtomicBoolean batchProcessingActive = new AtomicBoolean(false);
    private List<JsonNode> pendingEvents = new ArrayList<>();

    @Value("${event.integration.batch-size:1000}")
    private int batchSize;

    @Autowired
    public EventIntegrationService(
            EventRepository eventRepository,
            JdbcTemplate jdbcTemplate,
            ArtistService artistService,
            TagService tagService,
            PlaceIntegrationService placeIntegrationService,
            ArtistRelationshipService artistRelationshipService,
            TagRelationshipService tagRelationshipService)
    {
        this.artistRelationshipService = artistRelationshipService;
        this.eventRepository = eventRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.artistService = artistService;
        this.tagService = tagService;
        this.placeIntegrationService = placeIntegrationService;
        this.tagRelationshipService = tagRelationshipService;

        logger.info("EventIntegrationService initialized");
    }


    /**
     * Primary method for processing events from external sources.
     * Sets up batch processing of all events.
     *
     * @param events List of events as JSON nodes to process
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void processAllScrapedEvents(List<JsonNode> events) {
        if (events == null || events.isEmpty()) {
            logger.info("No events to process");
            return;
        }

        logger.info("Starting batch processing of {} events", events.size());

        // Store the events for processing
        this.pendingEvents = new ArrayList<>(events);
        this.currentOffset.set(0);

        // Process the first batch immediately
        processBatch();

        // Schedule subsequent batches to run
        scheduleRemainingBatches();
    }

    /**
     * Process a single batch of records based on the current offset.
     * Called both directly and through scheduling.
     */
    @Scheduled(fixedDelay = 10000) // Runs every 10 seconds when active
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatch() {
        // Only run if we have pending events and not already processing
        if (pendingEvents.isEmpty() || !batchProcessingActive.compareAndSet(false, true)) {
            return;
        }

        try {
            int start = currentOffset.get();
            int end = Math.min(start + batchSize, pendingEvents.size());

            if (start >= pendingEvents.size()) {
                logger.info("All batches completed. Total events processed: {}", pendingEvents.size());
                pendingEvents.clear();
                return;
            }

            List<JsonNode> currentBatch = pendingEvents.subList(start, end);
            logger.info("Processing batch {}-{} of {} events", start, end-1, pendingEvents.size());

            // Initialize URL cache
            initializeUrlCache();

            // Processing statistics for this batch
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger skipCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // Process in smaller chunks for performance
            for (int i = 0; i < currentBatch.size(); i += BATCH_SIZE) {
                int chunkEnd = Math.min(i + BATCH_SIZE, currentBatch.size());
                List<JsonNode> chunk = currentBatch.subList(i, chunkEnd);

                processChunk(chunk, successCount, skipCount, errorCount);

                // Clear the persistence context after each chunk
                entityManager.flush();
                entityManager.clear();
            }

            logger.info("Batch {}-{} completed. Success: {}, Skipped: {}, Errors: {}",
                    start, end-1, successCount.get(), skipCount.get(), errorCount.get());

            // Update the offset for the next batch
            currentOffset.set(end);

        } catch (Exception e) {
            logger.error("Error processing batch: {}", e.getMessage(), e);
        } finally {
            batchProcessingActive.set(false);
        }
    }

    /**
     * Process a single chunk of a batch with its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processChunk(List<JsonNode> chunk, AtomicInteger successCount,
                                AtomicInteger skipCount, AtomicInteger errorCount) {
        List<Event> processedEvents = new ArrayList<>();

        for (JsonNode eventNode : chunk) {
            try {
                String url = eventNode.get("url").asText();

                // Skip already processed events
                if (url == null || isEventProcessed(url)) {
                    skipCount.incrementAndGet();
                    continue;
                }

                Event processedEvent = processEventNode(eventNode);

                if (processedEvent != null) {
                    processedEvents.add(processedEvent);
                    successCount.incrementAndGet();
                } else {
                    skipCount.incrementAndGet();
                }

            } catch (Exception e) {
                logger.error("Error processing event: {}", e.getMessage(), e);
                errorCount.incrementAndGet();
            }
        }
    }

    /**
     * Ensures all batches get processed by scheduling them.
     */
    private void scheduleRemainingBatches() {
        // This method will trigger the scheduled processBatch method
        // to run until all batches are processed
        if (!pendingEvents.isEmpty() && currentOffset.get() < pendingEvents.size()) {
            batchProcessingActive.set(false); // Allow the scheduler to pick up the next batch
        }
    }

    /**
     * Process a single event node into a complete Event entity with relationships.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Event processEventNode(JsonNode eventNode) {
        try {
            String url = eventNode.get("url").asText();

            // Skip already processed events
            if (url == null || isEventProcessed(url)) {
                logger.debug("Event already processed or invalid URL: {}", url);
                return null;
            }

            // Create the event entity
            Event newEvent = createEventFromNode(eventNode);

            // Assign place information
            newEvent.setPlace(placeIntegrationService.getOrCreatePlace(eventNode));

            // Save the event first
            Event savedEvent = eventRepository.save(newEvent);

            // Process relationships
            processEventRelationships(savedEvent, eventNode);

            // Mark as processed
            markEventAsProcessed(url);

            return savedEvent;
        } catch (Exception e) {
            logger.error("Error processing event node: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create relationships between an event and its artists and tags.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void processEventRelationships(Event event, JsonNode eventNode) {
        try {
            // Process artists
            Set<String> artistNames = extractNames(eventNode, "artists");
            if (!artistNames.isEmpty()) {
                Map<String, Artist> artistMap = artistService.findOrCreateArtistsByNames(artistNames);
                List<Long> artistIds = artistMap.values().stream()
                        .map(Artist::getId)
                        .collect(Collectors.toList());
                artistRelationshipService.linkArtistsToEvent(event.getId(), artistIds);
            }

            // Process tags
            Set<String> tagNames = extractNames(eventNode, "tags");
            if (!tagNames.isEmpty()) {
                Map<String, Tag> tagMap = tagService.findOrCreateTagsByNames(tagNames);
                List<Long> tagIds = tagMap.values().stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                tagRelationshipService.linkTagsToEvent(event.getId(), tagIds);
            }
        } catch (Exception e) {
            logger.error("Error processing relationships for event {}: {}",
                    event.getEventName(), e.getMessage(), e);
        }
    }

    /**
     * Create an Event entity from a JSON node.
     */
    public Event createEventFromNode(JsonNode eventNode) {
        Event newEvent = new Event();

        // Set basic event properties
        JsonNode nameNode = eventNode.get("event_name");
        newEvent.setEventName(nameNode != null ? nameNode.asText() : "Unknown Event");

        // Handle dates with default values
        JsonNode startDateNode = eventNode.get("start_date");
        LocalDateTime startDate = startDateNode != null ? parseTimestamp(startDateNode.asText()) : null;
        newEvent.setStartDate(startDate != null ? startDate : LocalDateTime.now());

        JsonNode endDateNode = eventNode.get("end_date");
        LocalDateTime endDate = endDateNode != null ? parseTimestamp(endDateNode.asText()) : null;
        newEvent.setEndDate(endDate != null ? endDate : LocalDateTime.now().plusHours(1));

        JsonNode thumbnailNode = eventNode.get("thumbnail");
        newEvent.setThumbnail(thumbnailNode != null ? thumbnailNode.asText() : "No thumbnail");

        newEvent.setUrl(eventNode.get("url").asText());

        JsonNode locationNode = eventNode.get("location");
        newEvent.setLocation(locationNode != null ? locationNode.asText() : "Unknown Location");

        JsonNode categoryNode = eventNode.get("category");
        newEvent.setCategory(categoryNode != null ? categoryNode.asText() : "Unknown Category");

        JsonNode descriptionNode = eventNode.get("description");
        newEvent.setDescription(descriptionNode != null ? descriptionNode.asText() : "No description available");

        JsonNode sourceNode = eventNode.get("source");
        newEvent.setSource(sourceNode != null ? sourceNode.asText() : "Unknown Source");

        return newEvent;
    }

    /**
     * Initialize the cache of processed URLs from the database.
     */
    public void initializeUrlCache() {
        if (processedUrls.isEmpty()) {
            logger.info("Initializing processed URLs cache");

            List<String> existingUrls = jdbcTemplate.queryForList(
                    "SELECT url FROM events", String.class);

            processedUrls.addAll(existingUrls);
            logger.info("Loaded {} existing event URLs into cache", existingUrls.size());
        }
    }

    /**
     * Check if an event URL has already been processed.
     */
    public boolean isEventProcessed(String url) {
        return url != null && processedUrls.contains(url);
    }

    /**
     * Mark an event URL as processed.
     */
    public void markEventAsProcessed(String url) {
        if (url != null) {
            processedUrls.add(url);
        }
    }

    /**
     * Extract names (artists or tags) from a JSON node.
     */
    private Set<String> extractNames(JsonNode eventNode, String fieldName) {
        if (!eventNode.has(fieldName) || eventNode.get(fieldName).isNull()) {
            return Collections.emptySet();
        }

        String namesString = eventNode.get(fieldName).asText();
        return parseNames(namesString);
    }

    /**
     * Parse a comma-separated string into a set of trimmed, non-empty names.
     */
    private Set<String> parseNames(String namesString) {
        if (namesString == null || namesString.isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(namesString.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Parse a timestamp string to LocalDateTime.
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.equals("null")) {
            return null;
        }

        try {
            long epochSeconds = Long.parseLong(timestamp);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            logger.warn("Invalid timestamp format: {}", timestamp);
            return null;
        }
    }

    /**
     * Clear all caches in the system.
     */
    public void clearAllCaches() {
        clearUrlCache();
        artistService.clearCache();
        tagService.clearCache();
        placeIntegrationService.clearCache();
        logger.info("All integration service caches cleared");
    }

    /**
     * Clear the URL cache.
     */
    public void clearUrlCache() {
        processedUrls.clear();
        logger.info("URL cache cleared");
    }

    /**
     * Get comprehensive statistics from all components.
     */
    public Map<String, Object> getIntegrationStatus() {
        Map<String, Object> status = new HashMap<>();

        // Event statistics
        status.put("totalEvents", eventRepository.count());
        status.put("cachedUrls", processedUrls.size());
        status.put("pendingEvents", pendingEvents.size());
        status.put("currentBatchOffset", currentOffset.get());
        status.put("batchSize", batchSize);

        // Add statistics from other services
        status.putAll(artistService.getStatistics());
        status.putAll(tagService.getStatistics());
        status.putAll(placeIntegrationService.getStatistics());

        return status;
    }
}