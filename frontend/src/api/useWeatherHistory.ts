import { useEffect, useState } from 'react';
import api from './index';
import { WeatherReading } from './useWeatherForLocation';

export function useWeatherHistory(locationKey: string, days: number = 7) {
    const [history, setHistory] = useState<WeatherReading[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
        let cancelled = false;
        const fetchHistory = async () => {
            try {
                const end = new Date();
                const start = new Date(end.getTime() - days * 24 * 60 * 60 * 1000);
                const response = await api.get(`/weather/history/${locationKey}`, {
                    params: { startTime: start.toISOString().slice(0, 19), endTime: end.toISOString().slice(0, 19) },
                });
                if (!cancelled && Array.isArray(response.data)) {
                    setHistory(response.data);
                    setError(false);
                }
            } catch {
                if (!cancelled) setError(true);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        fetchHistory();
        const interval = setInterval(fetchHistory, 10 * 60 * 1000);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, [locationKey, days]);

    return { history, loading, error };
}
