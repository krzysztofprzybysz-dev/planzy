// Truncate text with ellipsis
export const truncateText = (text, maxLength = 100) => {
    if (!text) return '';

    return text.length > maxLength
        ? `${text.substring(0, maxLength).trim()}...`
        : text;
};

// Format currency
export const formatCurrency = (amount, currency = 'PLN', locale = 'pl-PL') => {
    if (amount === undefined || amount === null) return '';

    return new Intl.NumberFormat(locale, {
        style: 'currency',
        currency
    }).format(amount);
};

// Format number with thousands separator
export const formatNumber = (number, locale = 'pl-PL') => {
    if (number === undefined || number === null) return '';

    return new Intl.NumberFormat(locale).format(number);
};

// Convert new lines to paragraphs for display
export const newLinesToParagraphs = (text) => {
    if (!text) return [];

    return text.split('\n').map((paragraph, index) => ({
        id: index,
        content: paragraph.trim()
    })).filter(paragraph => paragraph.content);
};