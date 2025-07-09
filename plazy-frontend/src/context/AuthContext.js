import React, { createContext, useState, useEffect } from 'react';
import { loginUser, registerUser, getCurrentUser } from '../services/authService';

// Create the authentication context
export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    // State to hold user authentication status and info
    const [user, setUser] = useState(null);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Check if user is logged in on initial load
    useEffect(() => {
        const initAuth = async () => {
            try {
                // Check if token exists in localStorage
                const token = localStorage.getItem('token');

                if (token) {
                    // Get current user info if token exists
                    const userData = await getCurrentUser();
                    setUser(userData);
                    setIsAuthenticated(true);
                }
            } catch (err) {
                console.error('Error initializing auth:', err);
                // Clear potentially invalid tokens
                localStorage.removeItem('token');
                setUser(null);
                setIsAuthenticated(false);
            } finally {
                setLoading(false);
            }
        };

        initAuth();
    }, []);

    // Login handler
    const login = async (username, password) => {
        try {
            setError(null);
            const response = await loginUser(username, password);

            // Store token and user data
            localStorage.setItem('token', response.token);
            setUser({
                id: response.id,
                username: response.username,
                email: response.email,
                roles: response.roles
            });
            setIsAuthenticated(true);
            return true;
        } catch (err) {
            console.error('Login error:', err);
            setError(err.response?.data?.message || 'Błąd podczas logowania. Spróbuj ponownie.');
            return false;
        }
    };

    // Register handler
    const register = async (userData) => {
        try {
            setError(null);
            const response = await registerUser(userData);
            return { success: true, message: response.message };
        } catch (err) {
            console.error('Registration error:', err);
            setError(err.response?.data?.message || 'Błąd podczas rejestracji. Spróbuj ponownie.');
            return { success: false, message: err.response?.data?.message || 'Błąd podczas rejestracji' };
        }
    };

    // Logout handler
    const logout = () => {
        localStorage.removeItem('token');
        setUser(null);
        setIsAuthenticated(false);
    };

    // Provide the auth context to child components
    return (
        <AuthContext.Provider
            value={{
                user,
                isAuthenticated,
                loading,
                error,
                login,
                register,
                logout
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

// Custom hook to use the auth context
export const useAuth = () => {
    const context = React.useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};