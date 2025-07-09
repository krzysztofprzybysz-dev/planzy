package pl.planzy.embedding.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class OpenAiEmbeddingResponse {

    private String object;
    private List<EmbeddingData> data;
    private String model;
    private Usage usage;

    @Setter
    @Getter
    public static class EmbeddingData {

        private String object;
        private Integer index;
        private List<Float> embedding;

    }

    @Setter
    @Getter
    public static class Usage {

        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;

    }
}
