import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import AppRoutes from './routes';
import Navbar from './components/layout/Navbar';
import Footer from './components/layout/Footer';
import PageContainer from './components/layout/PageContainer';
import { AuthProvider } from './context/AuthContext';

function App() {
    return (
        <AuthProvider>
            <Router>
                <div className="app">
                    <Navbar />
                    <PageContainer>
                        <AppRoutes />
                    </PageContainer>
                    <Footer />
                </div>
            </Router>
        </AuthProvider>
    );
}

export default App;