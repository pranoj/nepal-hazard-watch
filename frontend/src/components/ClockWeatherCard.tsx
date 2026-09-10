import { useEffect, useState } from 'react';
import type { LucideIcon } from 'lucide-react';
import { Sun, Cloud, CloudRain, CloudDrizzle, CloudLightning, CloudSnow, CloudFog, CloudSun, MapPin, TriangleAlert } from 'lucide-react';
import { useWeatherForLocation } from '../api/useWeatherForLocation';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { glassCardPad, mutedText } from '../utils/theme';

const WEATHER_CONDITION_ICON: Record<string, { Icon: LucideIcon; color: string }> = {
    Clear: { Icon: Sun, color: '#fbbf24' },
    Clouds: { Icon: Cloud, color: '#cbd5e1' },
    Rain: { Icon: CloudRain, color: '#60a5fa' },
    Drizzle: { Icon: CloudDrizzle, color: '#60a5fa' },
    Thunderstorm: { Icon: CloudLightning, color: '#a78bfa' },
    Snow: { Icon: CloudSnow, color: '#e2e8f0' },
    Mist: { Icon: CloudFog, color: '#cbd5e1' },
    Fog: { Icon: CloudFog, color: '#cbd5e1' },
    Haze: { Icon: CloudFog, color: '#cbd5e1' },
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
    worst: GlofRiskAssessment | null;
}

export function ClockWeatherCard({ worst }: ClockWeatherCardProps) {
    const [now, setNow] = useState(new Date());
    // Dig Tsho is just the fallback before risk data has loaded.
    const locationKey = worst?.icimodId ?? '348';
    const locationLabel = worst?.lakeName ?? 'Dig Tsho';
    const { weather, error } = useWeatherForLocation(locationKey);

    useEffect(() => {
        const interval = setInterval(() => setNow(new Date()), 30_000);
        return () => clearInterval(interval);
    }, []);

    const { time, day } = formatNepalNow(now);
    const { Icon: WeatherIcon, color: weatherColor } =
        (weather && WEATHER_CONDITION_ICON[weather.weatherCondition]) ?? { Icon: CloudSun, color: '#cbd5e1' };

    return (
        <div style={glassCardPad}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <div style={{ fontSize: '1.6rem', fontWeight: 700, letterSpacing: '0.02em' }}>{time}</div>
                <div style={{ ...mutedText, fontSize: '0.75rem' }}>{day} · NPT</div>
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid rgba(255,255,255,0.15)', margin: '0.6rem 0' }} />

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <WeatherIcon size={32} strokeWidth={1.75} color={weatherColor} style={{ flexShrink: 0 }} />
                <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: '1.4rem', fontWeight: 700 }}>
                        {weather ? `${Math.round(weather.temperature)}°C` : '—'}
                    </div>
                    <div style={{ textTransform: 'capitalize', fontSize: '0.8rem' }}>
                        {weather?.description ?? 'Loading...'}
                    </div>
                </div>
            </div>
            <div style={{ ...mutedText, marginTop: '0.4rem', fontSize: '0.75rem', lineHeight: 1.4, display: 'flex', gap: '0.3rem' }}>
                <MapPin size={13} strokeWidth={2} style={{ flexShrink: 0, marginTop: '0.1rem' }} />
                <span>{locationLabel}</span>
            </div>
            {error && !weather && (
                <div style={{ marginTop: '0.4rem', fontSize: '0.72rem', color: '#f87171', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                    <TriangleAlert size={12} strokeWidth={2} /> Weather unavailable right now
                </div>
            )}
        </div>
    );
}
