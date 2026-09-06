import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { ALERT_COLORS } from '../utils/alertVisuals';

type AlertLevel = GlofRiskAssessment['alertLevel'];

// Same shapes as the map markers (utils/alertVisuals.ts), as JSX for lists/legends.
export function AlertShapeIcon({ level, size = 12 }: { level: AlertLevel; size?: number }) {
    const color = ALERT_COLORS[level];
    return (
        <svg width={size} height={size} viewBox="0 0 20 20" style={{ flexShrink: 0 }}>
            {level === 'NORMAL' && <circle cx={10} cy={10} r={8} fill={color} />}
            {level === 'WATCH' && <polygon points="10,1 19,18 1,18" fill={color} strokeLinejoin="round" />}
            {level === 'DANGER' && (
                <rect x={4} y={4} width={12} height={12} fill={color} transform="rotate(45 10 10)" />
            )}
            {level === 'EXTREME' && (
                <polygon
                    points="10,1 12.35,7.53 19.51,7.64 13.76,11.97 15.88,18.86 10,14.82 4.12,18.86 6.24,11.97 0.49,7.64 7.65,7.53"
                    fill={color}
                    strokeLinejoin="round"
                />
            )}
        </svg>
    );
}
