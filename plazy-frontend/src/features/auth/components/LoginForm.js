import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import Button from '../../../components/ui/Button';
import '../styles/AuthForms.css';

const LoginForm = () => {
    const { login, error } = useAuth();
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [formError, setFormError] = useState('');

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setFormError('');

        // Simple validation
        if (!formData.username.trim() || !formData.password) {
            setFormError('Wprowadź nazwę użytkownika i hasło');
            return;
        }

        try {
            setIsSubmitting(true);
            const loginSuccess = await login(formData.username, formData.password);

            if (loginSuccess) {
                // Redirect after successful login
                navigate('/');
            } else {
                setFormError('Niepoprawna nazwa użytkownika lub hasło');
            }
        } catch (err) {
            setFormError(err.message || 'Wystąpił błąd podczas logowania');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="auth-form-container">
            <h2>Zaloguj się</h2>
            <p className="auth-subtitle">Witaj z powrotem! Zaloguj się, aby odkrywać wydarzenia.</p>

            {(formError || error) && (
                <div className="auth-error">
                    {formError || error}
                </div>
            )}

            <form className="auth-form" onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="username">Nazwa użytkownika</label>
                    <input
                        type="text"
                        id="username"
                        name="username"
                        value={formData.username}
                        onChange={handleChange}
                        placeholder="Twoja nazwa użytkownika"
                        required
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="password">Hasło</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder="Twoje hasło"
                        required
                    />
                </div>

                <Button
                    type="submit"
                    fullWidth={true}
                    disabled={isSubmitting}
                >
                    {isSubmitting ? 'Logowanie...' : 'Zaloguj się'}
                </Button>
            </form>

            <div className="auth-links">
                <p>
                    Nie masz jeszcze konta? <Link to="/register">Zarejestruj się</Link>
                </p>
            </div>
        </div>
    );
};

export default LoginForm;