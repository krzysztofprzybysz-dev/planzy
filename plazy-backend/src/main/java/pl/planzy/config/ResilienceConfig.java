package pl.planzy.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowSize(100)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(10)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                                   // Maksymalna liczba prób
                .waitDuration(Duration.ofMillis(1000))            // Czas oczekiwania między próbami
                .retryExceptions(Exception.class)                 // Wyjątki, które powodują ponowienie próby
                .failAfterMaxAttempts(true)                       // Rzuć wyjątek po wyczerpaniu prób
                .build();

        return RetryRegistry.of(config);
    }
}