import React from 'react';
import { Loader2 } from 'lucide-react';

const Button = ({
                    children,
                    variant = 'primary',
                    size = 'md',
                    isLoading = false,
                    disabled = false,
                    icon: Icon,
                    className = '',
                    type = 'button',
                    onClick,
                    ...props
                }) => {

    const baseStyles = "inline-flex items-center justify-center font-semibold transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:opacity-60 disabled:cursor-not-allowed rounded-lg active:scale-[0.98]";

    const variants = {
        primary: "bg-orange-600 hover:bg-orange-700 text-white shadow-md shadow-orange-500/20 focus:ring-orange-500 border border-transparent",
        secondary: "bg-gray-800 hover:bg-gray-900 text-white shadow-sm focus:ring-gray-600 border border-transparent",
        outline: "bg-white hover:bg-gray-50 text-gray-700 border border-gray-300 focus:ring-gray-200 shadow-sm",
        ghost: "bg-transparent hover:bg-gray-100 text-gray-600 hover:text-gray-900 focus:ring-gray-200",
        danger: "bg-red-50 hover:bg-red-100 text-red-600 border border-transparent focus:ring-red-500",
    };

    const sizes = {
        sm: "px-3 py-1.5 text-xs",
        md: "px-4 py-2 text-sm",
        lg: "px-6 py-3 text-base",
        icon: "p-2 aspect-square"
    };

    const buttonClasses = `
    ${baseStyles}
    ${variants[variant] || variants.primary}
    ${sizes[size] || sizes.md}
    ${className}
  `.trim();

    return (
        <button
            type={type}
            className={buttonClasses}
            disabled={disabled || isLoading}
            onClick={onClick}
            {...props}
        >
            {isLoading && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
            {!isLoading && Icon && (
                <span className={`${children ? 'mr-2' : ''}`}>
          <Icon size={size === 'sm' ? 16 : 18} />
        </span>
            )}
            {children}
        </button>
    );
};

export default Button;