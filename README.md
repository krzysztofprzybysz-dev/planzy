# 🎯 Planzy: AI-Powered Event Recommendation System

An intelligent event aggregator and recommendation platform that uses AI-driven semantic search to connect users with events tailored to their unique preferences. Built with React, Spring Boot, and PostgreSQL (pgvector).

![Java](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk)
![Spring](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F.svg?style=for-the-badge&logo=spring)
![React](https://img.shields.io/badge/React-19-61DAFB.svg?style=for-the-badge&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1.svg?style=for-the-badge&logo=postgresql)
![OpenAI](https://img.shields.io/badge/OpenAI-text--embedding--3-412991?style=for-the-badge&logo=openai)
![Google Places API](https://img.shields.io/badge/Google_Places-API-4285F4?style=for-the-badge&logo=google)
![Playwright](https://img.shields.io/badge/Playwright-1.34-2EAD33?style=for-the-badge&logo=playwright)

## 📋 Overview

Planzy is a full-stack web application designed to solve the challenge of discovering relevant cultural, entertainment, and educational events. It functions as both a sophisticated **event aggregator** and an **intelligent recommendation system**.

The application automatically scrapes data from various external event portals (`eBilet`, `Going.`), normalizes it, and enriches it using external services like the Google Places API for detailed venue information and the OpenAI API for generating vector embeddings.

The core of Planzy is its **semantic search capability**. Instead of simple keyword matching, users can fill out a detailed preference survey, which is converted into a vector representation. This vector is then used to find the most semantically similar events in the database, offering highly personalized and nuanced recommendations that go beyond simple category filters.

## 🌟 Key Features

- **🤖 AI-Powered Recommendations:** Utilizes a multi-step "Virtual Assistant" survey to understand user preferences and deliver semantically matched event recommendations.
- **🔄 Automated Data Aggregation:** Asynchronously scrapes and merges event data from multiple sources using a combination of HTTP clients and browser automation with Playwright.
- **✨ Intelligent Data Enrichment:**
  - **Venue Details:** Integrates with the Google Places API to fetch and cache comprehensive venue information, including addresses, ratings, and popularity scores.
  - **Semantic Data:** Generates vector embeddings for all events using the OpenAI API to power semantic search.
- **🔎 Advanced Browsing & Filtering:** Offers a robust interface for users to browse, paginate, sort, and filter events by a variety of criteria like category, location, artist, and tags.
- **🛡️ Resilient Architecture:** Employs resilience patterns like Circuit Breaker and Retry (via Resilience4j) to ensure stability when interacting with external APIs.
- **🖥️ Responsive User Interface:** A clean and intuitive frontend built with React that provides a seamless user experience across all devices.
- **⚙️ Command-Line Management:** Includes an interactive CLI to manually trigger and manage the entire data pipeline, from scraping to embedding generation.

## 🖼️ Application Showcase

*(This section is reserved for UI screenshots and architecture diagrams)*

**Suggested Screenshots:**
- Main `EventsPage` showing the grid of `EventCard` components.
- The initial prompt of the `PreferenceSurvey` ("Virtual Assistant").
- An active step within the `PreferenceSurvey`.
- The page displaying personalized recommendation results.

**Suggested Diagrams:**
- High-level System Architecture Diagram (Frontend -> Backend -> Database -> External APIs).
- Data Pipeline Flowchart (Scraping -> Processing -> Enrichment -> Embedding -> Storage).
- Semantic Search Logic Diagram (User Survey -> Prompt Generation -> Query Embedding -> Vector Search).

## 🏗️ Architecture

Planzy is built on a modern, modular, and scalable architecture, separating the presentation layer from the business and data logic.

### Frontend (React)

The frontend is a single-page application (SPA) built with **React**.
- **Component-Based UI:** Leverages reusable components for consistent and maintainable UI elements (`EventCard`, `Button`, etc.).
- **State Management:** Uses React Context (`AuthContext`) for managing global state like user authentication.
- **Routing:** Employs `react-router-dom` for client-side routing.
- **API Communication:** A dedicated service layer (`api.js`) using Axios handles all communication with the backend, with interceptors for automatically attaching JWT tokens and handling auth errors.
- **Custom Hooks:** Encapsulates reusable logic, such as `useEvents` for fetching event data and `useRecommendations` for handling the survey and results.

### Backend (Spring Boot)

The backend is a robust application built with the **Spring Boot** framework, exposing a stateless REST API.
- **Modular Design:** The codebase is organized by domain (`event`, `artist`, `tag`, `place`, `embedding`), each with its own services, repositories, and exception handling for clear separation of concerns.
- **Data Persistence:** Uses **Spring Data JPA** to interact with a **PostgreSQL** database. The `pgvector` extension is used for storing and performing similarity searches on vector embeddings.
- **AI & External Services:**
  - `EmbeddingService`: Manages interaction with the OpenAI API to create embeddings from event data.
  - `GooglePlacesService`: Manages all calls to the Google Places API and includes resilience patterns with **Resilience4j**.
- **Data Integration Pipeline:**
  - `ScrapperService`: Orchestrates multiple scraper implementations in parallel using a `TaskExecutor`.
  - `EventIntegrationService`: Handles the complex logic of processing scraped JSON data, creating/updating entities, and managing relationships in batches.
- **Security:** Secured with **Spring Security**, using JWT for stateless authentication. Passwords are never stored in plain text and are hashed using `BCryptPasswordEncoder`.

## 🔧 Technical Details

### Semantic Search with Vector Embeddings

The "Virtual Assistant" feature is powered by a sophisticated semantic search pipeline:

1.  **Survey to Prompt:** User selections from the `PreferenceSurvey` are programmatically compiled into a detailed text prompt that describes their ideal event. (see `useRecommendations.js`)
2.  **Query Embedding:** This prompt is sent to the OpenAI API via the `EmbeddingService` to generate a 1536-dimension vector embedding that numerically represents the user's preferences.
3.  **Vector Search:** The backend uses a native SQL query with `pgvector` operators (e.g., `cosine_distance`) to compare the user's query vector against the pre-calculated vectors of all events stored in the database. (see `EmbeddingService.java`)
4.  **Ranked Results:** The database returns a list of event IDs ordered by semantic similarity (closest cosine distance), which are then fetched with all their details and presented to the user.

## 🚀 Getting Started

To get the application running locally, you need to set up both the backend and frontend.

### Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL with the `pgvector` extension installed
- API keys for OpenAI and Google Places API

### Backend Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/planzy-ai-event-recommender.git](https://github.com/your-username/planzy-ai-event-recommender.git)
    cd planzy-ai-event-recommender/backend
    ```

2.  **Configure Environment:**
    Set the following environment variables or update the `src/main/resources/application.properties` file:
    ```properties
    DB_URL=jdbc:postgresql://localhost:5432/your_db_name
    DB_USERNAME=your_db_user
    DB_PASSWORD=your_db_pass
    OPENAI_API_KEY=your_openai_api_key
    GOOGLE_MAPS_API_KEY=your_google_maps_api_key
    ```
    *Note: Ensure your PostgreSQL database has the `pgvector` extension enabled (`CREATE EXTENSION vector;`).*

3.  **Run the application:**
    ```bash
    ./mvnw spring-boot:run
    ```
    The backend will start on `http://localhost:8081`.

### Frontend Setup

1.  **Navigate to the frontend directory:**
    ```bash
    cd ../frontend
    ```

2.  **Install dependencies:**
    ```bash
    npm install
    ```

3.  **Start the development server:**
    ```bash
    npm start
    ```
    The frontend will open on `http://localhost:3000` and connect to the backend.

## 📝 Usage

### Application Usage

1.  Open the application in your browser.
2.  Browse the main list of events.
3.  To get personalized recommendations, click the **"Activate Event Assistant"** button on the `EventsPage`.
4.  Complete the multi-step survey to define your preferences.
5.  Upon submission, the system will display a list of events semantically matched to your taste.
6.  You can register and log in to access future personalized features.

### Data Pipeline CLI

The backend includes an interactive command-line interface to manage the data. After starting the backend application, use the console where it's running:

```bash 
== Planzy Management Console ==
1. Run full data pipeline (Scrape → Process → Generate Embeddings)
2. Scrape data only
3. Process scraped data
4. Generate embeddings for events
5. Test semantic search
6. Check pending embeddings count
7. Exit
Enter your choice:
```


