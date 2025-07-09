import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import RegisterForm from '../features/auth/components/RegisterForm';
import { useAuth } from '../context/AuthContext';

const RegisterPage = () => {
    const { isAuthenticated } = useAuth();
    const navigate = useNavigate();

    // Redirect if already logged in
    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }

        // Update page title
        document.title = 'Rejestracja | Planzy';
    }, [isAuthenticated, navigate]);

    return (
        <div className="register-page">
            <RegisterForm />
        </div>
    );
};

export default RegisterPage;