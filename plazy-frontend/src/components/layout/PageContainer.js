import React from 'react';
import './PageContainer.css';

const PageContainer = ({ children }) => {
    return (
        <main className="page-container">
            {children}
        </main>
    );
};

export default PageContainer;