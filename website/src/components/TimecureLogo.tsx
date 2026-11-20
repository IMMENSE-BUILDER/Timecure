import React from 'react';

interface TimecureLogoProps {
  size?: number;
  className?: string;
}

/**
 * Faithful vector rendition of Timecure's redesigned neural-orbit AI emblem
 * from MotionComponents.kt (TimecureAiLogoButton).
 */
export const TimecureLogo: React.FC<TimecureLogoProps> = ({
  size = 36,
  className = '',
}) => {
  return (
    <div
      className={`relative inline-flex items-center justify-center rounded-full ${className}`}
      style={{
        width: size,
        height: size,
        backgroundColor: 'var(--tc-primary-container)',
        boxShadow: 'inset 0 0 0 1px color-mix(in srgb, var(--tc-primary) 35%, transparent)',
      }}
      aria-hidden="true"
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 40 40"
        fill="none"
        className="overflow-visible"
      >
        {/* Rotating orbital arc */}
        <g className="origin-center animate-tc-orbit">
          <circle
            cx="20"
            cy="20"
            r="14.5"
            stroke="var(--tc-primary)"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeDasharray="62 29"
            opacity="0.9"
          />
          <circle cx="34.5" cy="20" r="2.1" fill="var(--tc-tertiary)" />
        </g>

        {/* Precision 4-point neural star */}
        <path
          d="M20 9.5L22.4 17.6L30.5 20L22.4 22.4L20 30.5L17.6 22.4L9.5 20L17.6 17.6L20 9.5Z"
          fill="var(--tc-on-primary-container)"
        />
        {/* Pulsing inner core */}
        <circle cx="20" cy="20" r="2.2" fill="var(--tc-tertiary)" />
      </svg>
    </div>
  );
};
