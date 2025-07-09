import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import Button from '../../../components/ui/Button';
import '../styles/AuthForms.css';

const RegisterForm = () => {
    const { register } = useAuth();
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        fullName: ''
    });

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [formError, setFormError] = useState('');
    const [success, setSuccess] = useState('');

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
        setSuccess('');

        // Validation
        if (!formData.username.trim()) {
            setFormError('Nazwa użytkownika jest wymagana');
            return;
        }

        if (!formData.email.trim() || !/\S+@\S+\.\S+/.test(formData.email)) {
            setFormError('Wprowadź poprawny adres email');
            return;
        }

        if (!formData.password || formData.password.length < 6) {
            setFormError('Hasło musi mieć co najmniej 6 znaków');
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            setFormError('Hasła nie są identyczne');
            return;
        }

        try {
            setIsSubmitting(true);

            const response = await register({
                username: formData.username,
                email: formData.email,
                password: formData.password,
                fullName: formData.fullName
            });

            if (response.success) {
                setSuccess('Rejestracja zakończona pomyślnie! Możesz się teraz zalogować.');
                // Reset form
                setFormData({
                    username: '',
                    email: '',
                    password: '',
                    confirmPassword: '',
                    fullName: ''
                });

                // Redirect to login after short delay
                setTimeout(() => {
                    navigate('/login');
                }, 2000);
            } else {
                setFormError(response.message || 'Błąd podczas rejestracji');
            }
        } catch (err) {
            setFormError(err.message || 'Wystąpił błąd podczas rejestracji');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="auth-form-container">
            <h2>Zarejestruj się</h2>
            <p className="auth-subtitle">Dołącz do nas, aby odkrywać najlepsze wydarzenia!</p>

            {formError && (
                <div className="auth-error">
                    {formError}
                </div>
            )}

            {success && (
                <div className="auth-success">
                    {success}
                </div>
            )}

            <form className="auth-form" onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="username">Nazwa użytkownika*</label>
                    <input
                        type="text"
                        id="username"
                        name="username"
                        value={formData.username}
                        onChange={handleChange}
                        placeholder="Wybierz nazwę użytkownika"
                        required
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="email">Email*</label>
                    <input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        placeholder="Twój adres email"
                        required
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="fullName">Imię i nazwisko (opcjonalnie)</label>
                    <input
                        type="text"
                        id="fullName"
                        name="fullName"
                        value={formData.fullName}
                        onChange={handleChange}
                        placeholder="Twoje imię i nazwisko"
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="password">Hasło*</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder="Hasło (min. 6 znaków)"
                        required
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="confirmPassword">Potwierdź hasło*</label>
                    <input
                        type="password"
                        id="confirmPassword"
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        placeholder="Powtórz hasło"
                        required
                    />
                </div>

                <Button
                    type="submit"
                    fullWidth={true}
                    disabled={isSubmitting}
                >
                    {isSubmitting ? 'Rejestracja...' : 'Zarejestruj się'}
                </Button>
            </form>

            <div className="auth-links">
                <p>
                    Masz już konto? <Link to="/login">Zaloguj się</Link>
                </p>
            </div>
        </div>
    );
};

export default RegisterForm;