package pl.planzy.embedding.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import pl.planzy.config.OpenAiConfig;
import pl.planzy.embedding.domain.OpenAiEmbeddingRequest;
import pl.planzy.embedding.domain.OpenAiEmbeddingResponse;
import pl.planzy.event.domain.Event;
import pl.planzy.event.domain.EventRepository;
import pl.planzy.place.domain.Place;
import pl.planzy.tag.domain.Tag;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int BATCH_SIZE = 20; // Process events in batches
    private static final String OPENAI_EMBEDDING_URL = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final OpenAiConfig openAiConfig;
    private final EventRepository eventRepository;

    @Autowired
    public EmbeddingService(@Qualifier("openAiRestTemplate") RestTemplate restTemplate,
                            JdbcTemplate jdbcTemplate,
                            OpenAiConfig openAiConfig,
                            EventRepository eventRepository) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.openAiConfig = openAiConfig;
        this.eventRepository = eventRepository;
    }

    /**
     * Call OpenAI API to create embeddings
     */
    private List<OpenAiEmbeddingResponse.EmbeddingData> createEmbeddings(List<String> texts) {
        try {
            // Prepare request
            OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest();
            request.setModel(openAiConfig.getEmbeddingModel());
            request.setInput(texts);
            request.setDimensions(openAiConfig.getEmbeddingDimensions());

            // Set up headers with API key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openAiConfig.getApiKey());

            HttpEntity<OpenAiEmbeddingRequest> entity = new HttpEntity<>(request, headers);

            // Make API call
            OpenAiEmbeddingResponse response = restTemplate.postForObject(
                    OPENAI_EMBEDDING_URL,
                    entity,
                    OpenAiEmbeddingResponse.class
            );

            // Log token usage
            if (response != null && response.getUsage() != null) {
                logger.info("OpenAI API usage: {} prompt tokens, {} total tokens",
                        response.getUsage().getPromptTokens(),
                        response.getUsage().getTotalTokens());
            }

            // Return embedding data
            if (response != null && response.getData() != null) {
                return response.getData();
            } else {
                logger.error("No embedding data returned from OpenAI API");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Prepares event text for embedding by combining relevant fields
     */
    private String prepareEventText(Event event) {
        StringBuilder text = new StringBuilder();

        // Event name (weighted by repetition)
        if (event.getEventName() != null) {
            String name = cleanText(event.getEventName());
            text.append("Event: ").append(name).append(". ");
            text.append("Title: ").append(name).append(". "); // Repeating for emphasis
        }

        // Category
        if (event.getCategory() != null) {
            text.append("Category: ").append(cleanText(event.getCategory())).append(". ");
        }

        // Artists
        String artists = getArtistNamesForEvent(event.getId());
        if (artists != null && !artists.isEmpty()) {
            text.append("Artists: ").append(cleanText(artists)).append(". ");
            text.append("Performers: ").append(cleanText(artists)).append(". "); // Synonym for better matching
        }

        // Tags
        String tags = getTagsForEvent(event.getId());
        if (tags != null && !tags.isEmpty()) {
            text.append("Tags: ").append(cleanText(tags)).append(". ");
        }

        // Basic location
        if (event.getLocation() != null) {
            text.append("Location: ").append(cleanText(event.getLocation())).append(". ");
        }

        // Place details
        Place place = event.getPlace();
        if (place != null) {
            // Place types (venue category information)
            if (place.getPlaceTypes() != null) {
                String placeTypes = cleanText(place.getPlaceTypes());
                text.append("Venue Type: ").append(placeTypes).append(". ");
            }

            // Place rating with review count context
            if (place.getRating() != null) {
                Double rating = place.getRating();
                Integer reviews = place.getUserRatingsTotal();

                text.append("Venue Rating: ").append(rating).append(" stars");
                if (reviews != null && reviews > 0) {
                    text.append(" based on ").append(reviews).append(" reviews");
                }
                text.append(". ");
            }

            // Place popularity
            if (place.getPopularityScore() != null) {
                Double score = place.getPopularityScore();
                text.append("Venue Popularity: ");

                if (score >= 90) {
                    text.append("extremely popular venue, ");
                } else if (score >= 80) {
                    text.append("highly popular venue, ");
                } else if (score >= 70) {
                    text.append("very popular venue, ");
                } else if (score >= 50) {
                    text.append("popular venue, ");
                } else {
                    text.append("venue with moderate popularity, ");
                }

                // Add city context if available
                if (place.getCity() != null) {
                    if (score >= 85) {
                        text.append("top-rated venue in ").append(place.getCity());
                    } else if (score >= 70) {
                        text.append("well-known venue in ").append(place.getCity());
                    } else {
                        text.append("venue in ").append(place.getCity());
                    }
                }

                text.append(". ");
            }
        }

        // Add time context for better temporal matching
        if (event.getStartDate() != null) {
            String timeContext = getTimeContext(event.getStartDate());
            if (!timeContext.isEmpty()) {
                text.append("Time: ").append(timeContext).append(". ");
            }
        }

        // Description - provides detailed semantic information
        if (event.getDescription() != null) {
            // Limit description length to avoid overwhelming other fields
            String description = cleanText(event.getDescription());
            if (description.length() > 1000) {
                description = description.substring(0, 1000);
            }
            text.append("Description: ").append(description);
        }

        logger.info("Prepared text for event ID {}: {}", event.getId(), text.toString());

        return text.toString().trim();
    }

    /**
     * Creates a time context description based on event date
     */
    private String getTimeContext(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        StringBuilder context = new StringBuilder();

        // Add day context
        DayOfWeek day = dateTime.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            context.append("weekend ");
        } else {
            context.append("weekday ");
        }

        // Add time of day
        int hour = dateTime.getHour();
        if (hour >= 5 && hour < 12) {
            context.append("morning ");
        } else if (hour >= 12 && hour < 17) {
            context.append("afternoon ");
        } else if (hour >= 17 && hour < 21) {
            context.append("evening ");
        } else {
            context.append("night ");
        }

        // Add season
        Month month = dateTime.getMonth();
        if (month == Month.DECEMBER || month == Month.JANUARY || month == Month.FEBRUARY) {
            context.append("winter");
        } else if (month == Month.MARCH || month == Month.APRIL || month == Month.MAY) {
            context.append("spring");
        } else if (month == Month.JUNE || month == Month.JULY || month == Month.AUGUST) {
            context.append("summer");
        } else {
            context.append("autumn");
        }

        return context.toString();
    }

    /**
     * Clean text for better embedding quality
     */
    private String cleanText(String text) {
        if (text == null) return "";

        // Replace multiple spaces, newlines, tabs with a single space
        text = text.replaceAll("\\s+", " ");

        // Remove special characters but keep common punctuation
        text = text.replaceAll("[^\\p{L}\\p{N}\\s.,!?'-]", "");

        return text.trim();
    }

    /**
     * Get artist names for an event from the database
     */
    private String getArtistNamesForEvent(Long eventId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT string_agg(a.artist_name, ', ') " +
                            "FROM artists a " +
                            "JOIN event_artists ea ON a.id = ea.artist_id " +
                            "WHERE ea.event_id = ?",
                    String.class,
                    eventId
            );
        } catch (Exception e) {
            logger.warn("Could not retrieve artist names for event {}: {}", eventId, e.getMessage());
            return "";
        }
    }

    /**
     * Get tags for an event from the database
     */
    private String getTagsForEvent(Long eventId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT string_agg(t.tag_name, ', ') " +
                            "FROM tags t " +
                            "JOIN event_tags et ON t.id = et.tag_id " +
                            "WHERE et.event_id = ?",
                    String.class,
                    eventId
            );
        } catch (Exception e) {
            logger.warn("Could not retrieve tags for event {}: {}", eventId, e.getMessage());
            return "";
        }
    }



    /**
     * Generate embeddings for events that don't have them yet.
     * Uses native queries to properly handle PostgreSQL vector type.
     */
    @Transactional
    public void generateEmbeddingsForNewEvents() {
        logger.info("Starting embedding generation for events without embeddings");

        // Get count of events needing embeddings
        int totalCount = eventRepository.countEventsWithoutEmbeddings();
        logger.info("Found {} events without embeddings", totalCount);

        if (totalCount == 0) {
            logger.info("No events found without embeddings. Process complete.");
            return;
        }

        // Get events without embeddings using native query
        List<Event> eventsWithoutEmbeddings = eventRepository.findEventsWithoutEmbeddingsNative(1000);
        logger.info("Retrieved {} events for processing", eventsWithoutEmbeddings.size());

        if (eventsWithoutEmbeddings.isEmpty()) {
            logger.info("No events found without embeddings. Process complete.");
            return;
        }

        // Process in batches to avoid API rate limits and memory issues
        List<List<Event>> batches = new ArrayList<>();
        for (int i = 0; i < eventsWithoutEmbeddings.size(); i += BATCH_SIZE) {
            batches.add(
                    eventsWithoutEmbeddings.subList(
                            i,
                            Math.min(i + BATCH_SIZE, eventsWithoutEmbeddings.size())
                    )
            );
        }

        AtomicInteger processedCount = new AtomicInteger(0);

        batches.forEach(batch -> {
            try {
                processBatch(batch);
                processedCount.addAndGet(batch.size());
                logger.info("Processed {}/{} events", processedCount.get(), eventsWithoutEmbeddings.size());
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error("Error processing batch: {}", e.getMessage(), e);
            }
        });

        logger.info("Embedding generation completed for all events");
    }

    /**
     * Process a batch of events to generate and store embeddings.
     * Uses JDBC with explicit type casting for PostgreSQL vector compatibility.
     */
    @Transactional
    protected void processBatch(List<Event> events) {
        if (events.isEmpty()) return;

        List<String> texts = new ArrayList<>();
        List<Long> eventIds = new ArrayList<>();

        // Prepare texts for embedding
        for (Event event : events) {
            String text = prepareEventText(event);
            texts.add(text);
            eventIds.add(event.getId());
        }

        // Generate embeddings with OpenAI API
        List<OpenAiEmbeddingResponse.EmbeddingData> embeddings = createEmbeddings(texts);

        if (embeddings.isEmpty()) {
            logger.error("Failed to generate embeddings for batch");
            return;
        }

        // Use JDBC for updating vector data directly
        for (int i = 0; i < embeddings.size(); i++) {
            try {
                // Convert the embedding to a PostgreSQL vector format
                String vectorString = convertToVectorString(embeddings.get(i).getEmbedding());

                // Use native SQL to update with proper casting
                jdbcTemplate.update(
                        "UPDATE events SET embedding = ?::vector WHERE id = ?",
                        vectorString,
                        eventIds.get(i)
                );

                logger.debug("Updated embedding for event ID: {}", eventIds.get(i));
            } catch (Exception e) {
                logger.error("Error saving embedding for event ID {}: {}",
                        eventIds.get(i), e.getMessage(), e);
            }
        }

        logger.info("Saved embeddings for {} events", embeddings.size());
    }

    /**
     * Convert a list of floats to PostgreSQL vector format: [0.1,0.2,0.3,...]
     */
    private String convertToVectorString(List<Float> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Find events similar to a text query using vector similarity search
     * @param queryText The text to find similar events for
     * @param limit Maximum number of events to return
     * @return List of events sorted by similarity to the query, with all relationships loaded
     */
    public List<Event> findSimilarEvents(String queryText, int limit) {
        // Generate embedding for the query
        List<OpenAiEmbeddingResponse.EmbeddingData> queryEmbeddings =
                createEmbeddings(Collections.singletonList(queryText));

        if (queryEmbeddings.isEmpty()) {
            logger.error("Failed to generate embedding for query");
            return Collections.emptyList();
        }

        // Format the query embedding as a PostgreSQL vector
        String vectorString = convertToVectorString(queryEmbeddings.get(0).getEmbedding());

        // Step 1: Use native SQL with PostgreSQL vector operators for similarity search
        List<Long> eventIds = jdbcTemplate.queryForList(
                "SELECT e.id " +
                        "FROM events e " +
                        "WHERE e.embedding IS NOT NULL " +
                        "ORDER BY cosine_distance(e.embedding, ?::vector(1536)) " +
                        "LIMIT ?",
                Long.class,
                vectorString, limit
        );

        if (eventIds.isEmpty()) {
            logger.info("No similar events found for query");
            return Collections.emptyList();
        }

        // Step 2: Efficiently load all events with their relationships in a single query
        List<Event> eventsWithRelationships = eventRepository.findByIdInWithRelationships(eventIds);

        // Step 3: Reorder to match the original similarity ordering from the vector search
        Map<Long, Event> eventMap = eventsWithRelationships.stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        // Create the final result list preserving the order from vector similarity search
        List<Event> result = eventIds.stream()
                .map(eventMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        logger.info("Returning {} events with relationships loaded", result.size());
        return result;
    }

    /**
     * Get tags for an event from the database
     */


    /**
     * Count the number of events that don't have embeddings yet
     * @return Count of events without embeddings
     */
    public int countPendingEmbeddings() {
        return eventRepository.countEventsWithoutEmbeddings();
    }
}