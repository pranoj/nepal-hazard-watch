import type { CSSProperties } from 'react';

// Shared frosted-glass card style for the dark dashboard theme.
export const glassCard: CSSProperties = {
    background: 'rgba(20, 26, 36, 0.55)',
    backdropFilter: 'blur(16px)',
    WebkitBackdropFilter: 'blur(16px)',
    border: '1px solid rgba(255, 255, 255, 0.12)',
    borderRadius: '18px',
    color: '#f4f6f8',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.35)',
};

export const glassCardPad: CSSProperties = {
    ...glassCard,
    padding: '1.25rem 1.5rem',
};

export const mutedText: CSSProperties = {
    color: 'rgba(244, 246, 248, 0.65)',
};

export const pill: CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 1rem',
    borderRadius: '999px',
    background: 'rgba(255, 255, 255, 0.08)',
    border: '1px solid rgba(255, 255, 255, 0.14)',
    color: '#f4f6f8',
    fontSize: '0.9rem',
};

// Ama Dablam, Nepal - Wikimedia Commons, CC-licensed.
export const BACKGROUND_IMAGE_URL =
    'https://upload.wikimedia.org/wikipedia/commons/c/cc/Ama_Dablam_under_a_Painted_Sky.jpg';
export const BACKGROUND_IMAGE_ATTRIBUTION = 'Ama Dablam, Nepal — Wikimedia Commons (CC)';

export const ALERT_LEVEL_LABEL: Record<'NORMAL' | 'WATCH' | 'DANGER' | 'EXTREME', string> = {
    NORMAL: 'Low',
    WATCH: 'Moderate',
    DANGER: 'High',
    EXTREME: 'Extreme',
};
