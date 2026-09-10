import { CloudRain, TriangleAlert } from 'lucide-react';
import { useWeatherHistory } from '../api/useWeatherHistory';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { glassCardPad, mutedText } from '../utils/theme';

interface RainfallTrendCardProps {
    worst: GlofRiskAssessment | null;
    isSelected: boolean;
}

// no real river gauge feed - shows daily rainfall bucketed from raw weather readings
export function RainfallTrendCard({ worst, isSelected }: RainfallTrendCardProps) {
    const locationKey = worst?.icimodId ?? '348';
    const locationLabel = worst?.lakeName ?? 'Dig Tsho';
    const { history, loading, error } = useWeatherHistory(locationKey, 7);

    const byDay = new Map<string, number>();
    for (const reading of history) {
        const day = reading.recordedAt.slice(0, 10);
        byDay.set(day, (byDay.get(day) ?? 0) + reading.rainfall);
    }
    const days = [...byDay.entries()].sort(([a], [b]) => a.localeCompare(b));
    const maxRain = Math.max(1, ...days.map(([, mm]) => mm));
    const latestTotal = days.length > 0 ? days[days.length - 1][1] : 0;

    return (
        <div style={glassCardPad}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                <CloudRain size={18} strokeWidth={2} color="#60a5fa" />
                <span>Rainfall Trend</span>
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 700 }}>{latestTotal.toFixed(1)} mm</div>
            <div style={{ ...mutedText, fontSize: '0.8rem', marginBottom: '0.75rem', lineHeight: 1.4 }}>
                Today, {locationLabel} ({isSelected ? 'selected' : 'highest risk'})
            </div>
            {loading && <div style={mutedText}>Loading...</div>}
            {!loading && error && days.length === 0 && (
                <div style={{ fontSize: '0.85rem', color: '#f87171', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <TriangleAlert size={14} strokeWidth={2} /> Rainfall data unavailable right now
                </div>
            )}
            {!loading && !error && days.length === 0 && (
                <div style={mutedText}>
                    No real rainfall history yet - our weather pipeline only started collecting
                    data on Sept 3, 2026.
                </div>
            )}
            {days.length > 0 && (
                <div style={{ display: 'flex', alignItems: 'flex-end', gap: '6px', height: '70px' }}>
                    {days.map(([day, mm]) => (
                        <div key={day} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1 }}>
                            <div
                                title={`${day}: ${mm.toFixed(1)}mm`}
                                style={{
                                    width: '100%',
                                    height: `${Math.max(4, (mm / maxRain) * 60)}px`,
                                    background: 'linear-gradient(180deg, #60a5fa, #2563eb)',
                                    borderRadius: '3px 3px 0 0',
                                }}
                            />
                            <div style={{ ...mutedText, fontSize: '0.65rem', marginTop: '0.25rem' }}>
                                {day.slice(5)}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
