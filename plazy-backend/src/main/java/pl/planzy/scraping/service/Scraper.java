package pl.planzy.scraping.service;

import com.fasterxml.jackson.databind.JsonNode;
import pl.planzy.scraping.mapper.EventMapper;


import java.util.List;

public interface Scraper {

    List<JsonNode> scrapeData();
    EventMapper getMapper();

}
