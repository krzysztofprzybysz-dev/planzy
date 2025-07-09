import { useState, useCallback } from 'react';
import { findSimilarEvents } from '../../events/api/eventApi'; // Adjust path if necessary

/**
 * Custom hook to manage state and logic for fetching event recommendations
 * based on user preferences.
 * @returns {object} State and functions for recommendations.
 */
const useRecommendations = () => {
    const [recommendations, setRecommendations] = useState([]);
    const [isRecommendationActive, setIsRecommendationActive] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [lastSurveyData, setLastSurveyData] = useState(null);

    /**
     * Formats the raw form data from the survey into a structured text prompt
     * suitable for the embedding model.
     * @param {object} formData - The raw data object from the PreferenceSurvey form.
     * @returns {string} A formatted text prompt string.
     */
    const formatSurveyDataToPrompt = useCallback((formData) => {
        let promptLines = ["Preferencje użytkownika:"];
        if (formData.eventTypes?.length > 0) promptLines.push(`Typ wydarzenia: ${formData.eventTypes.join(', ')}`);
        if (formData.genreTags?.trim()) promptLines.push(`Gatunek/Tagi: ${formData.genreTags.trim()}`);
        if (formData.artistPref && formData.artistPref !== 'Bez znaczenia') promptLines.push(`Artyści: ${formData.artistPref}`);
        if (formData.description?.trim()) promptLines.push(`Opis preferencji: ${formData.description.trim()}`);
        if (formData.location?.trim()) {
            let locationLine = `Miejsce: ${formData.location.trim()}`;
            if (formData.venueTypes?.length > 0 && !formData.venueTypes.includes('Dowolne')) {
                locationLine += `, preferowany typ: ${formData.venueTypes.join('/')}`;
            }
            promptLines.push(locationLine);
        }
        if (formData.timePref?.length > 0 && !formData.timePref.includes('Dowolny termin')) promptLines.push(`Kiedy: ${formData.timePref.join(' lub ')}`);
        if (formData.atmosphere?.length > 0) promptLines.push(`Atmosfera: ${formData.atmosphere.join(', ')}`);
        const finalPrompt = promptLines.length > 1 ? promptLines.join('\n') : '';
        console.log("[useRecommendations formatSurveyDataToPrompt] Generated Prompt:", finalPrompt || "(No preferences selected)");
        return finalPrompt;
    }, []);

    /**
     * Fetches event recommendations based on the provided survey data.
     * @param {object} formData - The raw data object from the PreferenceSurvey form.
     * @param {number} [limit=16] - The maximum number of recommendations to fetch.
     */
    const getRecommendations = useCallback(async (formData, limit = 16) => {
        console.log("[useRecommendations getRecommendations] Function called with data:", formData); // Log start
        const queryText = formatSurveyDataToPrompt(formData);

        if (!queryText) {
            console.log("[useRecommendations getRecommendations] No preferences selected, setting error.");
            setError("Proszę, wybierz przynajmniej jedną preferencję, aby znaleźć rekomendacje.");
            setRecommendations([]);
            setIsRecommendationActive(true);
            setLoading(false);
            setLastSurveyData(formData);
            return;
        }

        console.log("[useRecommendations getRecommendations] Setting loading state to true.");
        setLoading(true);
        setError(null);
        setLastSurveyData(formData);
        setIsRecommendationActive(true);

        try {
            console.log(`[useRecommendations getRecommendations] Calling findSimilarEvents with query: "${queryText}"`);
            const results = await findSimilarEvents(queryText, limit);
            console.log(`[useRecommendations getRecommendations] Received ${results?.length || 0} results from API.`);
            setRecommendations(results || []);
        } catch (err) {
            console.error('[useRecommendations getRecommendations] Error fetching recommendations:', err);
            setError('Nie udało się pobrać rekomendacji. Spróbuj ponownie później.');
            setRecommendations([]);
        } finally {
            console.log("[useRecommendations getRecommendations] Setting loading state to false.");
            setLoading(false);
        }
    }, [formatSurveyDataToPrompt]); // Dependency

    /**
     * Clears the current recommendations and deactivates the recommendation view.
     */
    const clearRecommendations = useCallback(() => {
        setRecommendations([]);
        setIsRecommendationActive(false);
        setError(null);
        setLastSurveyData(null);
        console.log("[useRecommendations clearRecommendations] Recommendations cleared.");
    }, []);

    return {
        recommendations,
        isRecommendationActive,
        loading,
        error,
        lastSurveyData,
        getRecommendations,
        clearRecommendations
    };
};

export default useRecommendations;
