import axios from 'axios';

// Create an instance of Axios with base URL
const api = axios.create({
    baseURL: 'http://127.0.0.1:8081/api',
});

// Request interceptor - add auth headers and other request processing
api.interceptors.request.use(
    (config) => {
        // Get token from localStorage
        const token = localStorage.getItem('token');

        // If token exists, add it to request headers
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor - handle common response patterns
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        // Handle common errors here (e.g., 401 unauthorized, 404 not found)
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            console.error('API Error:', error.response.status, error.response.data);

            // Handle specific status codes
            if (error.response.status === 401) {
                // Unauthorized - clear token and redirect to login
                console.error('Unauthorized access');
                localStorage.removeItem('token');
                localStorage.removeItem('user');

                // Redirect to login if we're in a browser context
                if (typeof window !== 'undefined') {
                    window.location.href = '/login';
                }
            }
        } else if (error.request) {
            // The request was made but no response was received
            console.error('Network Error:', error.request);
        } else {
            // Something happened in setting up the request that triggered an Error
            console.error('Request Error:', error.message);
        }

        return Promise.reject(error);
    }
);

export default api;