package pl.planzy.scraping.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.planzy.scraping.service.Scraper;
import pl.planzy.scraping.mapper.EventMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component("scrapperEbilet")
public class ScraperEbilet implements Scraper {

    private static final Logger logger = LoggerFactory.getLogger(ScraperEbilet.class);
    private final EventMapper eventMapper;

    @Autowired
    public ScraperEbilet(@Qualifier("eventMapperEbilet") EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    @Override
    public List<JsonNode> scrapeData() {

        List<JsonNode> scrappedData = new ArrayList<>();

        int size = 20;
        int top = 0;
        boolean hasNext = true;

        try {

            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            logger.info("[{}] Started fetching data ...", getClass().getSimpleName());

            while (hasNext) {

                String baseUrl = "https://www.ebilet.pl/api/TitleListing/Search";
                String url = String.format("%s?currentTab=2&sort=1&top=%d&size=%d", baseUrl, top, size);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode jsonNode = mapper.readTree(response.body());
                    JsonNode data = jsonNode.get("titles");

                    if (data != null && !data.isEmpty()) {
                        data.forEach(scrappedData::add);
                        top += size;
                    } else {
                        hasNext = false;
                    }

                } else {
                    logger.error("[{}] Failed to fetch data with HTTP status code: [{}]", getClass().getSimpleName(), response.statusCode());
                    hasNext = false;
                }
            }

        } catch (Exception e) {
            logger.error("[{}] An error occurred while scraping data ", getClass().getSimpleName(), e);
        }

        logger.info("[{}] Finished fetching. Total events fetched: [{}]", getClass().getSimpleName(), scrappedData.size());

        return scrappedData;
    }


    @Override
    public EventMapper getMapper() {
        return eventMapper;
    }
}