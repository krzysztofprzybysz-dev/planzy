package pl.planzy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pl.planzy.embedding.service.*;
import pl.planzy.event.domain.*;
import pl.planzy.event.service.*;
import pl.planzy.scraping.service.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Main application class for the Planzy platform.
 * Provides both web service functionality and command-line data processing capabilities.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class PlanzyApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(PlanzyApplication.class);

    private final ScrapperService scrapperService;
    private final EventIntegrationService eventIntegrationService;
    private final EmbeddingService embeddingService;

    @Autowired
    public PlanzyApplication(
            ScrapperService scrapperService,
            EventIntegrationService eventIntegrationService,
            EmbeddingService embeddingService) {
        this.scrapperService = scrapperService;
        this.eventIntegrationService = eventIntegrationService;
        this.embeddingService = embeddingService;
    }

    public static void main(String[] args) {
        SpringApplication.run(PlanzyApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Planzy application started");

        // Display interactive menu
        showMenu();
    }

    private void showMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("\n== Planzy Management Console ==");
            System.out.println("1. Run full data pipeline (Scrape → Process → Generate Embeddings)");
            System.out.println("2. Scrape data only");
            System.out.println("3. Process scraped data");
            System.out.println("4. Generate embeddings for events");
            System.out.println("5. Test semantic search");
            System.out.println("6. Check pending embeddings count");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        runFullPipeline();
                        break;
                    case "2":
                        runScraping();
                        break;
                    case "3":
                        runEventProcessing();
                        break;
                    case "4":
                        runEmbeddingGeneration();
                        break;
                    case "5":
                        testSemanticSearch(scanner);
                        break;
                    case "6":
                        checkPendingEmbeddings();
                        break;
                    case "7":
                        exit = true;
                        logger.info("Exiting Planzy application");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                        break;
                }
            } catch (Exception e) {
                logger.error("Error executing operation: {}", e.getMessage(), e);
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    private void runFullPipeline() {
        logger.info("Starting full data pipeline");
        System.out.println("Running full data pipeline (scrape → process → generate embeddings)...");

        // Step 1: Scrape data
        var scrapedData = scrapperService.scrapeAndMergeData();
        System.out.printf("Scraped %d events\n", scrapedData.size());

        // Step 2: Process events
        eventIntegrationService.processAllScrapedEvents(scrapedData);
        System.out.println("Events processed and saved to database");

        // Step 3: Generate embeddings
        embeddingService.generateEmbeddingsForNewEvents();
        System.out.println("Embeddings generated for events");

        logger.info("Full data pipeline completed successfully");
        System.out.println("Full data pipeline completed successfully!");
    }

    private void runScraping() {
        logger.info("Starting data scraping");
        System.out.println("Scraping data from configured sources...");

        var scrapedData = scrapperService.scrapeAndMergeData();
        System.out.printf("Scraping completed. Obtained %d events\n", scrapedData.size());

        logger.info("Data scraping completed: {} events scraped", scrapedData.size());
    }

    private void runEventProcessing() {
        logger.info("Starting event processing");
        System.out.println("Processing scraped events...");

        var scrapedData = scrapperService.scrapeAndMergeData();
        System.out.printf("Processing %d scraped events...\n", scrapedData.size());

        eventIntegrationService.processAllScrapedEvents(scrapedData);
        System.out.println("Events processed and saved to database");

        logger.info("Event processing completed");
    }

    private void runEmbeddingGeneration() {
        logger.info("Starting embedding generation for events");
        System.out.println("Generating embeddings for events without them...");

        int pendingCount = embeddingService.countPendingEmbeddings();
        System.out.printf("Found %d events that need embeddings\n", pendingCount);

        if (pendingCount > 0) {
            embeddingService.generateEmbeddingsForNewEvents();
            System.out.println("Embedding generation completed");
        } else {
            System.out.println("No events need embeddings at this time");
        }

        logger.info("Embedding generation process completed");
    }

    private void testSemanticSearch(Scanner scanner) {
        System.out.print("Enter search query (e.g., 'electronic music festival'): ");
        String query = scanner.nextLine();

        System.out.print("Number of results to return: ");
        int limit = Integer.parseInt(scanner.nextLine());

        System.out.printf("Searching for: '%s'...\n", query);

        List<Event> similarEvents = embeddingService.findSimilarEvents(query, limit);

        System.out.println("\nSearch Results:");
        System.out.println("===============");

        if (similarEvents.isEmpty()) {
            System.out.println("No matching events found");
        } else {
            int count = 1;
            for (Event event : similarEvents) {
                System.out.printf("%d. %s\n", count++, event.getEventName());
                System.out.printf("   Date: %s\n", event.getStartDate());
                System.out.printf("   Location: %s\n", event.getLocation());
                System.out.printf("   Category: %s\n", event.getCategory());
                System.out.println();
            }
        }

        logger.info("Semantic search completed for query: '{}', found {} results", query, similarEvents.size());
    }

    private void checkPendingEmbeddings() {
        int pendingCount = embeddingService.countPendingEmbeddings();
        System.out.printf("There are currently %d events waiting for embedding generation\n", pendingCount);
        logger.info("Pending embeddings count: {}", pendingCount);
    }
}