import api from './api';

// Handle user login
export const loginUser = async (username, password) => {
    try {
        const response = await api.post('/auth/login', {
            username,
            password
        });
        return response.data;
    } catch (error) {
        console.error('Login error:', error);
        throw error;
    }
};

// Handle user registration
export const registerUser = async (userData) => {
    try {
        const response = await api.post('/auth/register', userData);
        return response.data;
    } catch (error) {
        console.error('Registration error:', error);
        throw error;
    }
};

// Get current user information
export const getCurrentUser = async () => {
    // Get the token from local storage
    const token = localStorage.getItem('token');

    if (!token) {
        throw new Error('No authentication token found');
    }

    try {
        // This is a placeholder - in a real app, you might want to
        // create a dedicated endpoint to get current user details
        // For now, we'll just return the user from the token data in localStorage
        const userStr = localStorage.getItem('user');
        if (userStr) {
            return JSON.parse(userStr);
        }

        // If no user data in localStorage, can add API call here to fetch user data
        // Example:
        // const response = await api.get('/auth/me');
        // return response.data;

        throw new Error('No user data found');
    } catch (error) {
        console.error('Error getting current user:', error);
        throw error;
    }
};