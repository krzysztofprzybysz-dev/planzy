import { useState } from 'react';
import { findSimilarEvents } from '../../events/api/eventApi';

const useSearch = () => {
    const [searchResults, setSearchResults] = useState([]);
    const [lastQuery, setLastQuery] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isSearchActive, setIsSearchActive] = useState(false);

    const searchEvents = async (query, limit = 16) => {
        if (!query || query.trim() === '') {
            return [];
        }

        try {
            setLoading(true);
            setLastQuery(query);
            setIsSearchActive(true);
            setError(null);

            const results = await findSimilarEvents(query, limit);
            setSearchResults(results || []);

            return results;
        } catch (err) {
            console.error('Error searching events:', err);
            setError('Nie udało się znaleźć wydarzeń. Spróbuj ponownie później.');
            return [];
        } finally {
            setLoading(false);
        }
    };

    const clearSearch = () => {
        setSearchResults([]);
        setLastQuery('');
        setIsSearchActive(false);
        setError(null);
    };

    return {
        searchResults,
        lastQuery,
        loading,
        error,
        isSearchActive,
        searchEvents,
        clearSearch
    };
};

export default useSearch;