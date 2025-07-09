import React, { useState } from 'react';
import './EventCard.css';

const EventCard = ({ event, onClick }) => {
    const [imageLoaded, setImageLoaded] = useState(false);

    // Format date for display with improved error handling
    const formatEventDate = (dateString) => {
        if (!dateString) {
            return {
                weekday: 'N/A',
                time: 'N/A'
            };
        }

        try {
            const date = new Date(dateString);

            // Check if the date is valid
            if (isNaN(date.getTime())) {
                console.warn(`Invalid date string received: ${dateString}`);
                return {
                    weekday: 'N/A',
                    time: 'N/A'
                };
            }

            const weekday = new Intl.DateTimeFormat('pl-PL', { weekday: 'short' }).format(date);
            const time = date.toLocaleTimeString('pl-PL', {
                hour: '2-digit',
                minute: '2-digit',
            });

            return { weekday, time };
        } catch (error) {
            console.error(`Error formatting date: ${dateString}`, error);
            return {
                weekday: 'N/A',
                time: 'N/A'
            };
        }
    };

    // Calculate days until event with fixed date calculation
    const getDaysUntilEvent = (dateString) => {
        if (!dateString) return null;

        try {
            const now = new Date();
            const eventDate = new Date(dateString);

            // Check if the date is valid
            if (isNaN(eventDate.getTime())) {
                return null;
            }

            // Reset hours to compare only the dates
            const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
            const eventDay = new Date(eventDate.getFullYear(), eventDate.getMonth(), eventDate.getDate());

            // Calculate difference in days
            const diffTime = eventDay.getTime() - today.getTime();
            const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

            if (diffDays < 0) return "Wydarzenie minęło";
            if (diffDays === 0) return "Dzisiaj!";
            if (diffDays === 1) return "Jutro!";
            return `Za ${diffDays} dni`;
        } catch (error) {
            console.error(`Error calculating days until event: ${dateString}`, error);
            return null;
        }
    };

    // Truncate description
    const truncateDescription = (text, maxLength = 250) => {
        if (!text) return 'Brak opisu';
        return text.length > maxLength
            ? `${text.substring(0, maxLength).trim()}...`
            : text;
    };

    // Handle card click
    const handleCardClick = () => {
        if (onClick) onClick(event.id);
    };

    // Handle image load
    const handleImageLoad = () => {
        setImageLoaded(true);
    };

    // Format date info with safety checks
    const dateInfo = formatEventDate(event?.startDate);
    const daysUntil = getDaysUntilEvent(event?.startDate);

    // Filter out Unknown Artist entries completely
    const validArtists = event?.artists?.filter(
        artist => artist?.artistName &&
            artist.artistName.trim() !== "" &&
            artist.artistName !== "Unknown Artist"
    ) || [];

    return (
        <div
            className="event-card"
            onClick={handleCardClick}
            tabIndex={0}
            role="button"
            aria-label={`Wyświetl szczegóły wydarzenia: ${event?.eventName || 'Wydarzenie'}`}
        >
            <div className="event-card-image-container">
                <div className={`event-card-image-overlay ${imageLoaded ? 'loaded' : ''}`}>
                    <img
                        src={event?.thumbnail || 'https://via.placeholder.com/400x300?text=Brak+Obrazu'}
                        alt={event?.eventName || 'Wydarzenie'}
                        className="event-card-image"
                        onLoad={handleImageLoad}
                        onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = 'https://via.placeholder.com/400x300?text=Brak+Obrazu';
                        }}
                    />
                </div>

                {event?.category && (
                    <div className="event-category-tag">
                        {event.category}
                    </div>
                )}
            </div>

            <div className="event-card-content">
                {/* Front content (visible by default) */}
                <div className="event-main-content">
                    <div className="event-content-top">
                        <div className="event-time-info">
                            <div className="event-day">{dateInfo.weekday}</div>
                            <div className="event-time">{dateInfo.time}</div>
                            {daysUntil && <div className="event-countdown">{daysUntil}</div>}
                        </div>

                        <h3 className="event-title">{event?.eventName || 'Brak nazwy wydarzenia'}</h3>

                        <div className="event-location">
                            <i className="location-icon">📍</i>
                            <span>{event?.location || 'Brak lokalizacji'}</span>
                        </div>

                        {/* Only show artists section if there are valid artists */}
                        {validArtists.length > 0 && (
                            <div className="event-artists">
                                <i className="artist-icon">👤</i>
                                <span>
                                    {validArtists.slice(0, 2).map(a => a.artistName).join(', ')}
                                    {validArtists.length > 2 && ` +${validArtists.length - 2} więcej`}
                                </span>
                            </div>
                        )}
                    </div>

                    {/* Tags always at the bottom */}
                    <div className="event-tags-container">
                        {event?.tags && event.tags.length > 0 && (
                            <div className="event-tags">
                                {event.tags.slice(0, 3).map(tag => (
                                    <span key={tag.id} className="event-tag">
                                        {tag.tagName}
                                    </span>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Back content (visible on hover - description) */}
                <div className="event-description-content">
                    <h4>O wydarzeniu:</h4>
                    <p>{truncateDescription(event?.generatedDescription || event?.description)}</p>
                    <div className="event-hover-cta">Kliknij aby zobaczyć więcej</div>
                </div>
            </div>
        </div>
    );
};

export default EventCard;