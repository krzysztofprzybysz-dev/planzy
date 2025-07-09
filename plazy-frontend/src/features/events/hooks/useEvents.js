import { useState, useEffect, useCallback } from 'react';
import { getEvents } from '../api/eventApi';

const useEvents = (initialPage = 0, initialSize = 16, initialFilters = {}) => {
    const [events, setEvents] = useState([]);
    const [totalEvents, setTotalEvents] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [currentPage, setCurrentPage] = useState(initialPage);
    const [pageSize] = useState(initialSize);
    const [filters, setFilters] = useState(initialFilters);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchEvents = useCallback(async () => {
        try {
            setLoading(true);
            const data = await getEvents(currentPage, pageSize, filters);

            setEvents(data.content || []);
            setTotalPages(data.totalPages || 0);
            setTotalEvents(data.totalElements || 0);
            setError(null);
        } catch (err) {
            console.error('Error fetching events:', err);
            setError('Nie udało się pobrać wydarzeń. Spróbuj ponownie później.');
        } finally {
            setLoading(false);
        }
    }, [currentPage, pageSize, filters]);

    useEffect(() => {
        fetchEvents();
    }, [fetchEvents]);

    const handlePageChange = (newPage) => {
        setCurrentPage(newPage);
        // Scroll to top when changing page
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const updateFilters = (newFilters) => {
        setFilters(current => ({ ...current, ...newFilters }));
        setCurrentPage(0); // Reset to first page when filters change
    };

    const resetFilters = () => {
        setFilters(initialFilters);
        setCurrentPage(0);
    };

    return {
        events,
        totalEvents,
        totalPages,
        currentPage,
        loading,
        error,
        handlePageChange,
        updateFilters,
        resetFilters,
        refresh: fetchEvents,
    };
};

export default useEvents;