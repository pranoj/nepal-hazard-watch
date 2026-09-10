import { useEffect, useState } from 'react';
import api from './index';

export interface WeatherReading {
    location: string;
    temperature: number;
    humidity: number;
    rainfall: number;
    windSpeed: number;
    weatherCondition: string;
    description: string;
    recordedAt: string;
}

export function useWeatherForLocation(locationKey: string) {
    const [weather, setWeather] = useState<WeatherReading | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
        let cancelled = false;
        const fetchWeather = async () => {
            try {
                const response = await api.get(`/weather/${locationKey}`);
                if (!cancelled && response.data && response.data.temperature !== undefined) {
                    setWeather(response.data);
                    setError(false);
                }
            } catch {
                if (!cancelled) setError(true);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        fetchWeather();
        const interval = setInterval(fetchWeather, 5 * 60 * 1000);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, [locationKey]);

    return { weather, loading, error };
}
