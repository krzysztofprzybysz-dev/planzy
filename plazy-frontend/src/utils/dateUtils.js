// Format date for display
export const formatDate = (dateString, locale = 'pl-PL') => {
    if (!dateString) return 'Data niedostępna';

    const date = new Date(dateString);

    return date.toLocaleDateString(locale, {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
};

// Format time for display
export const formatTime = (dateString, locale = 'pl-PL') => {
    if (!dateString) return '';

    const date = new Date(dateString);

    return date.toLocaleTimeString(locale, {
        hour: '2-digit',
        minute: '2-digit'
    });
};

// Format date as day and month
export const formatShortDate = (dateString, locale = 'pl-PL') => {
    if (!dateString) return { weekday: 'N/A', time: 'N/A' };

    const date = new Date(dateString);
    const weekday = new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(date);
    const time = date.toLocaleTimeString(locale, {
        hour: '2-digit',
        minute: '2-digit'
    });

    return { weekday, time };
};

// Calculate days until event
export const getDaysUntilEvent = (dateString) => {
    if (!dateString) return null;

    const now = new Date();
    const eventDate = new Date(dateString);
    const timeDiff = eventDate.getTime() - now.getTime();
    const days = Math.ceil(timeDiff / (1000 * 3600 * 24));

    if (days < 0) return "Wydarzenie minęło";
    if (days === 0) return "Dzisiaj!";
    if (days === 1) return "Jutro!";
    return `Za ${days} dni`;
};

// Check if date is in the past
export const isDateInPast = (dateString) => {
    if (!dateString) return false;

    const now = new Date();
    const date = new Date(dateString);

    return date < now;
};