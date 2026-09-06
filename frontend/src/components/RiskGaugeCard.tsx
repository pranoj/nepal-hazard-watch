import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { ALERT_COLORS } from '../utils/alertVisuals';
import { ALERT_LEVEL_LABEL } from '../utils/theme';
import { findHighestRisk } from '../utils/risk';

interface RiskGaugeCardProps {
    risks: GlofRiskAssessment[];
}

// Shows the single highest risk score across every monitored point.
export function RiskGaugeCard({ risks }: RiskGaugeCardProps) {
    const worst = findHighestRisk(risks);

    const percent = worst ? Math.round(worst.riskScore) : 0;
    const level = worst?.alertLevel ?? 'NORMAL';
    const color = ALERT_COLORS[level];

    const radius = 30;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference * (1 - percent / 100);

    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', maxWidth: '340px' }}>
            <svg width={64} height={64} viewBox="0 0 72 72" style={{ flexShrink: 0 }}>
                <circle cx={36} cy={36} r={radius} fill="none" stroke="rgba(255,255,255,0.15)" strokeWidth={7} />
                <circle
                    cx={36} cy={36} r={radius} fill="none" stroke={color} strokeWidth={7}
                    strokeDasharray={circumference} strokeDashoffset={offset} strokeLinecap="round"
                    transform="rotate(-90 36 36)"
                    style={{ transition: 'stroke-dashoffset 0.6s ease' }}
                />
                <text x={36} y={40} textAnchor="middle" fontSize="15" fontWeight={700} fill="#f4f6f8">
                    {percent}%
                </text>
            </svg>
            <div style={{ minWidth: 0 }}>
                <div style={{ fontSize: '0.7rem', color: 'rgba(244,246,248,0.6)' }}>Highest current risk</div>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color }}>{ALERT_LEVEL_LABEL[level]}</div>
                {worst && (
                    <div style={{ fontSize: '0.7rem', color: 'rgba(244,246,248,0.55)', lineHeight: 1.3 }}>
                        {worst.lakeName}
                    </div>
                )}
            </div>
        </div>
    );
}
