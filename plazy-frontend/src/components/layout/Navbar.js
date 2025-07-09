import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);
    const location = useLocation();
    const { isAuthenticated, user, logout } = useAuth();

    // Toggle mobile menu
    const toggleMobileMenu = () => {
        setMobileMenuOpen(!mobileMenuOpen);
    };

    // Handle logout
    const handleLogout = () => {
        logout();
        setMobileMenuOpen(false);
    };

    // Add scroll event listener to change navbar style on scroll
    useEffect(() => {
        const handleScroll = () => {
            setScrolled(window.scrollY > 50);
        };

        window.addEventListener('scroll', handleScroll);
        return () => {
            window.removeEventListener('scroll', handleScroll);
        };
    }, []);

    // Close mobile menu on route change
    useEffect(() => {
        setMobileMenuOpen(false);
    }, [location]);

    // Check if a link is active
    const isActive = (path) => {
        return location.pathname === path;
    };

    return (
        <nav className={`navbar ${scrolled ? 'navbar-scrolled' : ''}`}>
            <div className="navbar-container">
                <Link to="/" className="navbar-logo">
                    <span className="logo-icon">🎭</span>
                    <span className="logo-text">Planzy</span>
                </Link>

                {/* Mobile menu button */}
                <button
                    className={`mobile-menu-button ${mobileMenuOpen ? 'open' : ''}`}
                    onClick={toggleMobileMenu}
                    aria-label="Przełącz menu"
                >
                    <span></span>
                    <span></span>
                    <span></span>
                </button>

                {/* Desktop Navigation */}
                <div className="navbar-links">
                    <Link to="/" className={`nav-link ${isActive('/') ? 'active' : ''}`}>
                        Wydarzenia
                    </Link>

                    {/* Show appropriate auth links based on authentication status */}
                    {isAuthenticated ? (
                        <>
                            <span className="nav-greeting">Cześć, {user?.username || 'Użytkowniku'}</span>
                            <button onClick={handleLogout} className="nav-link nav-logout">
                                Wyloguj się
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className={`nav-link ${isActive('/login') ? 'active' : ''}`}>
                                Zaloguj się
                            </Link>
                            <Link to="/register" className={`nav-link nav-register ${isActive('/register') ? 'active' : ''}`}>
                                Zarejestruj się
                            </Link>
                        </>
                    )}
                </div>
            </div>

            {/* Mobile Navigation */}
            <div className={`mobile-menu ${mobileMenuOpen ? 'open' : ''}`}>
                <Link to="/" className={`mobile-nav-link ${isActive('/') ? 'active' : ''}`}>
                    Wydarzenia
                </Link>

                {/* Show appropriate auth links based on authentication status */}
                {isAuthenticated ? (
                    <>
                        <span className="mobile-nav-greeting">Cześć, {user?.username || 'Użytkowniku'}</span>
                        <button onClick={handleLogout} className="mobile-nav-link mobile-nav-logout">
                            Wyloguj się
                        </button>
                    </>
                ) : (
                    <>
                        <Link to="/login" className={`mobile-nav-link ${isActive('/login') ? 'active' : ''}`}>
                            Zaloguj się
                        </Link>
                        <Link to="/register" className={`mobile-nav-link ${isActive('/register') ? 'active' : ''}`}>
                            Zarejestruj się
                        </Link>
                    </>
                )}
            </div>
        </nav>
    );
};

export default Navbar;