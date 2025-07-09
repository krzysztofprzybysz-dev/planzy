import React, { useEffect, useCallback } from 'react'; // Added useCallback
import { useNavigate } from 'react-router-dom';
import useEvents from '../features/events/hooks/useEvents';
import useRecommendations from '../features/recommendations/hooks/useRecommendations'; // Use the recommendation hook
import PreferenceSurvey from '../features/recommendations/components/PreferenceSurvey'; // Import the multi-step survey
import EventGrid from '../features/events/components/EventGrid';
import LoadingSpinner from '../components/ui/LoadingSpinner';
import Button from '../components/ui/Button';
import './EventsPage.css';

/**
 * Renders the main page displaying events.
 * Allows users to browse all events with pagination or get personalized recommendations
 * via the multi-step PreferenceSurvey component.
 */
const EventsPage = () => {
    const navigate = useNavigate();

    // Hook for fetching the default list of events
    const {
        events,
        totalEvents,
        totalPages,
        currentPage,
        loading: eventsLoading,
        error: eventsError,
        handlePageChange,
        // refresh: refreshEvents // Optional
    } = useEvents(0, 16);

    // Hook for managing recommendations
    const {
        recommendations,
        loading: recoLoading,
        error: recoError,
        isRecommendationActive,
        getRecommendations,
        clearRecommendations,
        lastSurveyData
    } = useRecommendations();

    useEffect(() => {
        document.title = 'Planzy - Wydarzenia';
    }, []);

    // Handler for clicking on an event card
    const handleEventClick = (eventId) => {
        // navigate(`/events/${eventId}`); // TODO: Implement event detail page
        console.log(`Navigate to event details for ID: ${eventId}`);
    };

    /**
     * Callback function passed to PreferenceSurvey.
     * Triggered when the survey form is submitted on the final step.
     * Wrapped in useCallback to prevent unnecessary re-renders of PreferenceSurvey.
     * @param {object} formData - The data collected from the survey form.
     */
    const handleSurveySubmit = useCallback(async (formData) => {
        console.log("[EventsPage handleSurveySubmit] Received data:", formData);
        await getRecommendations(formData);
        console.log("[EventsPage handleSurveySubmit] getRecommendations finished.");
        // Scroll down slightly to show results after submission
        // Find the header element to scroll to, or a default position
        const headerElement = document.querySelector('.events-page-header');
        if (headerElement) {
            headerElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else {
            window.scrollTo({ top: 300, behavior: 'smooth' }); // Fallback scroll position
        }
    }, [getRecommendations]); // Dependency on getRecommendations from the hook

    // Determine which set of events and loading/error states to display
    const displayedEvents = isRecommendationActive ? recommendations : events;
    const isLoading = isRecommendationActive ? recoLoading : eventsLoading;
    // Show recommendation error if active, otherwise show general event fetch error
    const displayedError = isRecommendationActive ? recoError : eventsError;

    return (
        <div className="events-page">

            {/* Multi-Step Preference Survey Component */}
            <PreferenceSurvey
                onSurveySubmit={handleSurveySubmit} // Pass the memoized callback
                totalEvents={totalEvents}
                isLoading={recoLoading}
                error={recoError}
            />

            {/* Page Header - Changes based on view mode */}
            <div className="events-page-header">
                {isRecommendationActive ? (
                    <>
                        <h1>Rekomendacje dla Ciebie</h1>
                        {!isLoading && !displayedError && (
                            <p>
                                Znaleziono {recommendations.length} pasujących wydarzeń.
                                <button onClick={clearRecommendations} className="reset-search-button">
                                    Pokaż wszystkie wydarzenia
                                </button>
                            </p>
                        )}
                        {/* Show error only if recommendations were active and failed */}
                        {displayedError && !isLoading && (
                            <p className="recommendation-error-message">{displayedError}</p>
                        )}
                    </>
                ) : (
                    <>
                        <h1>Odkryj wydarzenia</h1>
                        <p>
                            Znajdź wspaniałe wydarzenia
                            {totalEvents > 0 && !eventsLoading && ` • ${totalEvents} dostępnych`}
                        </p>
                    </>
                )}
            </div>

            {/* Content Area: Loading Spinner, Error Message, or Event Grid */}
            {isLoading ? (
                <LoadingSpinner text={isRecommendationActive ? "Szukam rekomendacji..." : "Ładowanie wydarzeń..."} />
            ) : displayedError && !isRecommendationActive ? ( // Show general error only in default view
                <div className="error-container">
                    <p>{displayedError}</p>
                    <Button onClick={() => handlePageChange(0)}>
                        Spróbuj ponownie
                    </Button>
                </div>
            ) : (
                <>
                    <EventGrid
                        events={displayedEvents}
                        onEventClick={handleEventClick}
                        emptyMessage={isRecommendationActive
                            ? "Nie znaleźliśmy rekomendacji pasujących do Twoich preferencji. Spróbuj zmienić kryteria w ankiecie powyżej."
                            : "Nie znaleźliśmy żadnych nadchodzących wydarzeń."
                        }
                    />

                    {/* Pagination - Show only for the default event list */}
                    {!isRecommendationActive && totalPages > 1 && (
                        <div className="pagination-container">
                            <Button
                                variant="outline"
                                disabled={currentPage === 0}
                                onClick={() => handlePageChange(currentPage - 1)}
                            >
                                ← Poprzednia
                            </Button>
                            <div className="pagination-info">
                                Strona {currentPage + 1} z {totalPages}
                            </div>
                            <Button
                                variant="outline"
                                disabled={currentPage >= totalPages - 1}
                                onClick={() => handlePageChange(currentPage + 1)}
                            >
                                Następna →
                            </Button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default EventsPage;
