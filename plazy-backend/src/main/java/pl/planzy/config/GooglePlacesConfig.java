package pl.planzy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class GooglePlacesConfig {

    @ConfigurationProperties(prefix = "google.places")
    @Bean
    public GooglePlacesProperties googlePlacesProperties() {
        return new GooglePlacesProperties();
    }

    @Setter
    @Getter
    public static class GooglePlacesProperties {

        private boolean enrichmentEnabled = false;
        private int refreshDays = 30;
        private String apiKey;
        private long requestDelay = 200;
        private String cron = "0 0 2 * * ?";

    }
}