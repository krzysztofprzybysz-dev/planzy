package pl.planzy.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${openai.embedding.dimensions:1536}")
    private int embeddingDimensions;

    @Bean(name = "openAiRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}