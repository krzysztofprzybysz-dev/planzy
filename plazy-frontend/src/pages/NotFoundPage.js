import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/ui/Button';
import './NotFoundPage.css';

const NotFoundPage = () => {
    useEffect(() => {
        document.title = 'Nie znaleziono strony | Planzy';
    }, []);

    return (
        <div className="not-found-page">
            <div className="not-found-content">
                <h1>404</h1>
                <h2>Nie znaleziono strony</h2>
                <p>Przepraszamy, strona której szukasz nie istnieje lub została przeniesiona.</p>
                <Link to="/">
                    <Button>Powrót do strony głównej</Button>
                </Link>
            </div>
        </div>
    );
};

export default NotFoundPage;