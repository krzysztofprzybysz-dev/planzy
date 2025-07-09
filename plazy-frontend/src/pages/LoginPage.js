import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginForm from '../features/auth/components/LoginForm';
import { useAuth } from '../context/AuthContext';

const LoginPage = () => {
    const { isAuthenticated } = useAuth();
    const navigate = useNavigate();

    // Redirect if already logged in
    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }

        // Update page title
        document.title = 'Logowanie | Planzy';
    }, [isAuthenticated, navigate]);

    return (
        <div className="login-page">
            <LoginForm />
        </div>
    );
};

export default LoginPage;