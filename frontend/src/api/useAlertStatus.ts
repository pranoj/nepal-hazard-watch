import { useEffect, useState } from 'react';

interface EarthquakeInfo {
    id: number;
    magnitude: number;
    latitude: number;
    longitude: number;
    eventTime: string;
    description: string;
    riskAssessment: string;
}

interface AlertStatusResponse {
    alertActive: boolean;
    minutesSinceAlert: number;
    newAlert: EarthquakeInfo;
    lastEarthquake: EarthquakeInfo | null;
}

export function useAlertStatus() {
    const [alertStatus, setAlertStatus] = useState<AlertStatusResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchAlertStatus = async () => {
            try {
                const response = await fetch('http://localhost:8080/api/latest-earthquake/alert-status');
                if (!response.ok) throw new Error('Failed to fetch alert status');
                const data = await response.json();
                setAlertStatus(data);
                setError(null);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Unknown error');
                console.error('Error fetching alert status:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchAlertStatus();
        const interval = setInterval(fetchAlertStatus, 30000);

        return () => clearInterval(interval);
    }, []);

    return { alertStatus, loading, error };
}