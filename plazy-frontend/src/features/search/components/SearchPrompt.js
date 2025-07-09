import React, { useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
import LoginPrompt from '../../auth/components/LoginPrompt';
import './SearchPrompt.css';

const SearchPrompt = ({ onSearch, totalEvents = 0 }) => {
    const [query, setQuery] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);
    const { isAuthenticated } = useAuth();

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!query.trim()) return;

        setIsLoading(true);
        setHasSearched(true);

        // If user is not authenticated, we don't actually perform the search
        // But we still set hasSearched to true to show the login prompt
        if (isAuthenticated) {
            onSearch(query)
                .finally(() => {
                    setIsLoading(false);
                });
        } else {
            // Simulate a delay before showing login prompt
            setTimeout(() => {
                setIsLoading(false);
            }, 500);
        }
    };

    return (
        <div className="search-prompt-container">
            <div className="search-prompt-text">
                <h2>Wirtualny asystent pomoże Ci znaleźć idealne wydarzenie</h2>
                <p>
                    Widzisz, ile mamy wydarzeń ({totalEvents})? Trudno się zdecydować?
                    Nasz wirtualny asystent pomoże Ci wybrać coś dopasowanego do Twoich preferencji!
                </p>
            </div>

            <form className="search-prompt-form" onSubmit={handleSubmit}>
                <input
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Opisz czego szukasz, np. koncert jazzowy w weekend, warsztaty dla dzieci, wystawa sztuki współczesnej..."
                    className="search-prompt-input"
                />
                <button
                    type="submit"
                    className="search-prompt-button"
                    disabled={isLoading || !query.trim()}
                >
                    {isLoading ? 'Wyszukiwanie...' : 'Znajdź dopasowane wydarzenia'}
                </button>
            </form>

            {/* Show login prompt if user has attempted a search but is not authenticated */}
            {hasSearched && !isAuthenticated && !isLoading && (
                <LoginPrompt />
            )}
        </div>
    );
};

export default SearchPrompt;