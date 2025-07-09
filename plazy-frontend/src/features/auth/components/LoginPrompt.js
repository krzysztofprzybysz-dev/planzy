import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../../../components/ui/Button';
import './LoginPrompt.css';

const LoginPrompt = () => {
    return (
        <div className="login-prompt">
            <div className="login-prompt-content">
                <div className="login-prompt-icon">🔒</div>
                <h3>Zaloguj się, aby zobaczyć dopasowane wyniki</h3>
                <p>
                    Aby zobaczyć personalizowane rekomendacje wydarzeń,
                    musisz zalogować się na swoje konto lub utworzyć nowe.
                </p>
                <div className="login-prompt-actions">
                    <Link to="/login">
                        <Button variant="primary">Zaloguj się</Button>
                    </Link>
                    <Link to="/register">
                        <Button variant="outline">Zarejestruj się</Button>
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default LoginPrompt;