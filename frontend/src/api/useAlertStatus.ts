import { useEffect, useState } from 'react';
import api from './index';
import { jitter } from '../utils/jitter';

export interface EarthquakeInfo {
    id: number;
    magnitude: number;
    latitude: number;
    longitude: number;
    eventTime: string;
    description: string;
    riskAssessment: string;
    sourceType: string | null;
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
                const response = await api.get<AlertStatusResponse>('/latest-earthquake/alert-status');
                setAlertStatus(response.data);
                setError(null);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Unknown error');
                console.error('Error fetching alert status:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchAlertStatus();
        const interval = setInterval(fetchAlertStatus, jitter(30000));

        return () => clearInterval(interval);
    }, []);

    return { alertStatus, loading, error };
}