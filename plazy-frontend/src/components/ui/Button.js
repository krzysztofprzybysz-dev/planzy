import React from 'react';
import './Button.css';

const Button = ({
                    children,
                    variant = 'primary',
                    size = 'medium',
                    fullWidth = false,
                    disabled = false,
                    onClick,
                    type = 'button',
                    className = '',
                    ...props
                }) => {
    return (
        <button
            type={type}
            className={`btn btn-${variant} btn-${size} ${fullWidth ? 'btn-full-width' : ''} ${className}`}
            disabled={disabled}
            onClick={onClick}
            {...props}
        >
            {children}
        </button>
    );
};

export default Button;