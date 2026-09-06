import { useEffect, useState } from 'react';
import { useWeatherForLocation } from '../api/useWeatherForLocation';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { glassCardPad, mutedText } from '../utils/theme';

const WEATHER_CONDITION_ICON: Record<string, string> = {
    Clear: '☀️',
    Clouds: '☁️',
    Rain: '🌧️',
    Drizzle: '🌦️',
    Thunderstorm: '⛈️',
    Snow: '❄️',
    Mist: '🌫️',
    Fog: '🌫️',
    Haze: '🌫️',
};

function formatNepalNow(date: Date): { time: string; day: string } {
    const time = date.toLocaleTimeString('en-US', {
        timeZone: 'Asia/Kathmandu', hour: 'numeric', minute: '2-digit',
    });
    const day = date.toLocaleDateString('en-US', {
        timeZone: 'Asia/Kathmandu', weekday: 'short', day: 'numeric', month: 'short',
    });
    return { time, day };
}

interface ClockWeatherCardProps {
    /** Weather follows the current highest-risk point, not a fixed lake. */
    worst: GlofRiskAssessment | null;
}

export function ClockWeatherCard({ worst }: ClockWeatherCardProps) {
    const [now, setNow] = useState(new Date());
    // Dig Tsho is just the fallback before risk data has loaded.
    const locationKey = worst?.icimodId ?? '348';
    const locationLabel = worst?.lakeName ?? 'Dig Tsho';
    const { weather } = useWeatherForLocation(locationKey);

    useEffect(() => {
        const interval = setInterval(() => setNow(new Date()), 30_000);
        return () => clearInterval(interval);
    }, []);

    const { time, day } = formatNepalNow(now);
    const icon = weather ? WEATHER_CONDITION_ICON[weather.weatherCondition] ?? '🌤️' : '🌤️';

    return (
        <div style={glassCardPad}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <div style={{ fontSize: '1.6rem', fontWeight: 700, letterSpacing: '0.02em' }}>{time}</div>
                <div style={{ ...mutedText, fontSize: '0.75rem' }}>{day} · NPT</div>
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid rgba(255,255,255,0.15)', margin: '0.6rem 0' }} />

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span style={{ fontSize: '1.8rem' }}>{icon}</span>
                <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: '1.4rem', fontWeight: 700 }}>
                        {weather ? `${Math.round(weather.temperature)}°C` : '—'}
                    </div>
                    <div style={{ textTransform: 'capitalize', fontSize: '0.8rem' }}>
                        {weather?.description ?? 'Loading...'}
                    </div>
                </div>
            </div>
            <div style={{ ...mutedText, marginTop: '0.4rem', fontSize: '0.75rem', lineHeight: 1.4 }}>
                📍 {locationLabel} — highest current risk
            </div>
        </div>
    );
}
