import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import EventsPage from './pages/EventsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import NotFoundPage from './pages/NotFoundPage';
import { useAuth } from './context/AuthContext';

// Protected route component
const ProtectedRoute = ({ children }) => {
    const { isAuthenticated, loading } = useAuth();

    // Show nothing while checking authentication
    if (loading) {
        return null;
    }

    // Redirect to login if not authenticated
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    // Render children if authenticated
    return children;
};

const AppRoutes = () => {
    return (
        <Routes>
            <Route path="/" element={<EventsPage />} />
            <Route path="/events" element={<Navigate to="/" replace />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
};

export default AppRoutes;