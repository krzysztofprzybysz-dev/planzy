import React from 'react';
import { Link } from 'react-router-dom';
import './Footer.css';

const Footer = () => {
    const currentYear = new Date().getFullYear();

    return (
        <footer className="footer">
            <div className="footer-container">
                <div className="footer-content">
                    <div className="footer-column">
                        <div className="footer-logo">
                            <span className="logo-icon">🎭</span>
                            <span className="logo-text">Planzy</span>
                        </div>
                        <p className="footer-tagline">
                            Twoja platforma do odkrywania i uczestniczenia w niesamowitych wydarzeniach
                        </p>
                        <div className="social-links">
                            <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" aria-label="Facebook">
                                <i className="social-icon">FB</i>
                            </a>
                            <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" aria-label="Twitter">
                                <i className="social-icon">TW</i>
                            </a>
                            <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" aria-label="Instagram">
                                <i className="social-icon">IG</i>
                            </a>
                        </div>
                    </div>

                    <div className="footer-column">
                        <h3>Szybkie linki</h3>
                        <ul className="footer-links">
                            <li><Link to="/">Wydarzenia</Link></li>
                            {/* Additional links can be added here */}
                        </ul>
                    </div>

                    <div className="footer-column">
                        <h3>Kategorie</h3>
                        <ul className="footer-links">
                            <li><Link to="/?category=Music">Muzyka</Link></li>
                            <li><Link to="/?category=Sports">Sport</Link></li>
                            <li><Link to="/?category=Arts">Sztuka</Link></li>
                            <li><Link to="/?category=FoodDrink">Jedzenie i napoje</Link></li>
                            <li><Link to="/?category=Technology">Technologia</Link></li>
                        </ul>
                    </div>

                    <div className="footer-column">
                        <h3>Kontakt</h3>
                        <address className="contact-info">
                            <p><i className="icon-location"></i> ul. Wydarzeniowa 123, Warszawa</p>
                            <p><i className="icon-email"></i> info@planzy.com</p>
                            <p><i className="icon-phone"></i> +48 123 456 789</p>
                        </address>
                        <div className="newsletter">
                            <h4>Zapisz się do newslettera</h4>
                            <div className="newsletter-form">
                                <input type="email" placeholder="Twój email" />
                                <button>Zapisz się</button>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="footer-bottom">
                    <p>&copy; {currentYear} Planzy. Wszelkie prawa zastrzeżone.</p>
                    <div className="footer-bottom-links">
                        <Link to="/terms">Regulamin</Link>
                        <Link to="/privacy">Polityka prywatności</Link>
                        <Link to="/cookies">Polityka cookies</Link>
                    </div>
                </div>
            </div>
        </footer>
    );
};

export default Footer;