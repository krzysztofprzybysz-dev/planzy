package pl.planzy.scraping.mapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface EventMapper {

    List<JsonNode> mapEvents(List<JsonNode> data);

}
