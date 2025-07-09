package pl.planzy.embedding.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class OpenAiEmbeddingRequest {

    private String model;
    private List<String> input;
    private Integer dimensions;

}

