import React from 'react';
import EventCard from './EventCard';
import './EventGrid.css';

const EventGrid = ({ events, onEventClick, emptyMessage = 'Nie znaleziono wydarzeń' }) => {
    if (!events || events.length === 0) {
        return (
            <div className="event-grid-empty">
                <p>{emptyMessage}</p>
            </div>
        );
    }

    return (
        <div className="event-grid">
            {events.map((event) => (
                <div key={event.id} className="event-grid-item">
                    <EventCard
                        event={event}
                        onClick={() => onEventClick(event.id)}
                    />
                </div>
            ))}
        </div>
    );
};

export default EventGrid;