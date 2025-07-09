import React from 'react';
import './LoadingSpinner.css';

const LoadingSpinner = ({ size = 'medium', text = 'Ładowanie...' }) => {
    return (
        <div className="loading-container">
            <div className={`loading-spinner ${size}`}></div>
            {text && <p className="loading-text">{text}</p>}
        </div>
    );
};

export default LoadingSpinner;