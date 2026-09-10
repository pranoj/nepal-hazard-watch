import L from 'leaflet';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';

type AlertLevel = GlofRiskAssessment['alertLevel'];

// shape as well as color, so levels stay distinguishable at small sizes and for color-blind users
export const ALERT_COLORS: Record<AlertLevel, string> = {
    NORMAL: '#16a34a',
    WATCH: '#eab308',
    DANGER: '#ea580c',
    EXTREME: '#dc2626',
};

function starPoints(cx: number, cy: number, outerR: number, innerR: number, points: number): string {
    const coords: string[] = [];
    for (let i = 0; i < points * 2; i++) {
        const r = i % 2 === 0 ? outerR : innerR;
        const angle = (Math.PI / points) * i - Math.PI / 2;
        coords.push(`${(cx + r * Math.cos(angle)).toFixed(2)},${(cy + r * Math.sin(angle)).toFixed(2)}`);
    }
    return coords.join(' ');
}

// shared 0-20 viewBox so the map icon and the small list dot render the exact same silhouette
export function alertShapeInnerSvg(level: AlertLevel, color: string): string {
    switch (level) {
        case 'NORMAL':
            return `<circle cx="10" cy="10" r="8" fill="${color}" stroke="white" stroke-width="1.5"/>`;
        case 'WATCH':
            return `<polygon points="10,1 19,18 1,18" fill="${color}" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>`;
        case 'DANGER':
            return `<rect x="4" y="4" width="12" height="12" fill="${color}" stroke="white" stroke-width="1.5" transform="rotate(45 10 10)"/>`;
        case 'EXTREME':
            return `<polygon points="${starPoints(10, 10, 9, 4, 5)}" fill="${color}" stroke="white" stroke-width="1.2" stroke-linejoin="round"/>`;
    }
}

export function alertShapeSvgMarkup(level: AlertLevel, color: string, size: number): string {
    return `<svg width="${size}" height="${size}" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">${alertShapeInnerSvg(level, color)}</svg>`;
}

// Leaflet stacks markers by screen position, not severity - this forces severity to always win (1000-pt gap beats the 0-100 tiebreak)
const ALERT_Z_BASE: Record<AlertLevel, number> = {
    NORMAL: 0,
    WATCH: 1000,
    DANGER: 2000,
    EXTREME: 3000,
};

export function alertZIndexOffset(level: AlertLevel, riskScore: number): number {
    return ALERT_Z_BASE[level] + Math.round(riskScore);
}

export function alertDivIcon(level: AlertLevel, riskScore: number): L.DivIcon {
    const size = Math.round(16 + riskScore / 4);
    return L.divIcon({
        html: alertShapeSvgMarkup(level, ALERT_COLORS[level], size),
        className: '',
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
        popupAnchor: [0, -size / 2],
    });
}
