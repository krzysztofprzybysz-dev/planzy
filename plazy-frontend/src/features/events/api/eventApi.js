import api from '../../../services/api';

// Function to get events from the backend with filters and pagination
export const getEvents = async (page = 0, size = 20, filters = {}) => {
    const { category, location, artist, tag, sort = 'start_date', direction = 'asc' } = filters;

    try {
        const response = await api.get('/events', {
            params: {
                page,
                size,
                category,
                location,
                artist,
                tag,
                sort,
                direction,
            },
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching events:', error);
        throw error;
    }
};

// Get a single event by ID
export const getEventById = async (id) => {
    try {
        const response = await api.get(`/events/${id}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching event details:', error);
        throw error;
    }
};


// Find similar events based on query
export const findSimilarEvents = async (query, limit = 16) => {
    try {
        const response = await api.get('/events/similar', {
            params: { query, limit }
        });
        return response.data;
    } catch (error) {
        console.error('Error finding similar events:', error);
        throw error;
    }
};
