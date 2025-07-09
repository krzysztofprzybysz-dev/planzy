package pl.planzy.scraping.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component("eventMapperGoingApp")
public class EventMapperGoingApp implements EventMapper {

    private static final Logger logger = LoggerFactory.getLogger(EventMapperGoingApp.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<JsonNode> mapEvents(List<JsonNode> data) {
        logger.info("[{}] Starting to map events. Total events to map: [{}]", getClass().getSimpleName(), data.size());

        List<JsonNode> mappedEvents = new ArrayList<>();

        for (JsonNode event : data) {
            try {
                mappedEvents.add(mapGoingAppEvent(event));
            } catch (Exception e) {
                logger.error("[{}] Error mapping event: [{}]", getClass().getSimpleName(), event, e);
            }
        }

        logger.info("[{}] Finished mapping events. Total mapped events: [{}]", getClass().getSimpleName(), mappedEvents.size());

        return mappedEvents;
    }

    private JsonNode mapGoingAppEvent(JsonNode event) {
        // Handle start date timestamp, converting from milliseconds to seconds if needed
        String startDateTimestamp = "null";
        if (event.has("start_date_timestamp") && !event.get("start_date_timestamp").isNull()) {
            startDateTimestamp = convertTimestamp(event.get("start_date_timestamp").asText());
        }

        // Handle end date timestamp, converting from milliseconds to seconds if needed
        String endDateTimestamp = "null";
        if (event.has("end_date_timestamp") && !event.get("end_date_timestamp").isNull()) {
            endDateTimestamp = convertTimestamp(event.get("end_date_timestamp").asText());
        }

        String artistsNames = event.has("artists_names") && event.get("artists_names").isArray()
                ? String.join(", ", mapper.convertValue(event.get("artists_names"), List.class))
                : "Unknown Artist";

        String location = event.has("locations_names") && event.get("locations_names").isArray() && event.get("locations_names").size() > 0
                ? event.get("locations_names").get(0).asText()
                : "Unknown Location";

        String url = event.has("slug") && event.has("rundate_slug")
                ? "https://queue.goingapp.pl/wydarzenie/" + event.get("slug").asText() + "/" + event.get("rundate_slug").asText()
                : "Unknown URL";

        String tags = event.has("tags_names") && event.get("tags_names").isArray()
                ? String.join(", ", mapper.convertValue(event.get("tags_names"), List.class))
                : "";

        // Fix thumbnail URL formatting with proper special character handling
        String thumbnail = "Unknown Thumbnail";
        if (event.has("thumbnail") && !event.get("thumbnail").isNull()) {
            String thumbnailPath = event.get("thumbnail").asText();

            // Properly encode special characters in path segments
            String encodedPath = encodeThumbnailPath(thumbnailPath);

            thumbnail = "https://res.cloudinary.com/dr89d8ldb/image/upload/c_fill,h_350,w_405/f_webp/q_auto:eco/v1/" + encodedPath;


            logger.debug("[{}] Formatted thumbnail URL: {}", getClass().getSimpleName(), thumbnail);
        }

        String normalizedTag = normalizeTags(tags);

        return mapper.createObjectNode()
                .put("event_name", event.has("name_pl") && !event.get("name_pl").isNull() ? event.get("name_pl").asText() : "Unknown Event")
                .put("artists", artistsNames)
                .put("start_date", startDateTimestamp)
                .put("end_date", endDateTimestamp)
                .put("thumbnail", thumbnail)
                .put("url", url)
                .put("location", location)
                .put("place", event.has("place_name") && !event.get("place_name").isNull() ? event.get("place_name").asText() : "Unknown Place")
                .put("category", event.has("category_name") && !event.get("category_name").isNull() ? event.get("category_name").asText() : "Unknown Category")
                .put("tags", normalizedTag)
                .put("description", event.has("description_pl") && !event.get("description_pl").isNull() ? event.get("description_pl").asText() : "No Description")
                .put("source", "GoingApp");
    }

    /**
     * Encodes a thumbnail path properly for URLs by encoding special characters
     * while preserving path structure (/ characters).
     *
     * @param path The thumbnail path to encode
     * @return The properly encoded path
     */
    private String encodeThumbnailPath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // Split the path by '/' to preserve path structure
        String[] segments = path.split("/");
        StringBuilder encodedPath = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encodedPath.append("/");
            }

            // URL encode each segment
            try {
                String encoded = URLEncoder.encode(segments[i], StandardCharsets.UTF_8.toString());
                // Replace '+' with '%20' for proper URL path encoding (not form encoding)
                encoded = encoded.replace("+", "%20");
                encodedPath.append(encoded);
            } catch (Exception e) {
                // Fallback to basic encoding if URLEncoder fails
                encodedPath.append(segments[i].replace(" ", "%20"));
                logger.warn("[{}] Error encoding path segment '{}': {}",
                        getClass().getSimpleName(), segments[i], e.getMessage());
            }
        }

        return encodedPath.toString();
    }

    /**
     * Converts timestamp strings to epoch seconds format.
     * GoingApp provides timestamps in milliseconds, but our system expects seconds.
     *
     * @param timestampStr The timestamp string from the API
     * @return Converted timestamp in epoch seconds
     */
    private String convertTimestamp(String timestampStr) {
        try {
            // Parse the timestamp string to a long
            long timestamp = Long.parseLong(timestampStr);

            // Check if the timestamp is in milliseconds (more than 10 digits)
            if (timestampStr.length() > 10) {
                // Convert from milliseconds to seconds by dividing by 1000
                timestamp = timestamp / 1000;
                logger.debug("[{}] Converted timestamp from milliseconds to seconds: {} -> {}",
                        getClass().getSimpleName(), timestampStr, timestamp);
            }

            return String.valueOf(timestamp);
        } catch (NumberFormatException e) {
            logger.error("[{}] Error parsing timestamp: {}", getClass().getSimpleName(), timestampStr, e);
            return "null";
        }
    }

    // Metoda normalizująca tagi
    private String normalizeTags(String tagsString) {
        if (tagsString == null || tagsString.isEmpty()) {
            return "";
        }

        // Podziel string tagów po przecinkach
        String[] tagArray = tagsString.split(",");
        List<String> normalizedTags = new ArrayList<>();

        for (String tag : tagArray) {
            String normalizedTag = normalizeTag(tag.trim());
            if (!normalizedTag.isEmpty()) {
                normalizedTags.add(normalizedTag);
            }
        }

        // Połącz znormalizowane tagi z powrotem w string
        return String.join(", ", normalizedTags);
    }

    private String normalizeTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return "";
        }

        // Normalizacja podstawowa: małe litery, usunięcie nadmiarowych spacji
        String normalized = tag.toLowerCase().trim().replaceAll("\\s+", " ");

        // Standaryzacja znaków specjalnych
        normalized = normalized.replace("-", " ").replace("_", " ");

        // Usuwanie znaków niepożądanych
        normalized = normalized.replaceAll("[^a-ząćęłńóśźż0-9 ]", "");

        return normalized;
    }
}