import { useState, useEffect } from 'react';
import api from './index';

export interface HazardEvent {
    id: number;
    regionId: number;
    eventType: string;
    status: string;
    eventTime: string;
    latitude: number;
    longitude: number;
    magnitude: number;
    description: string;
    deathToll: number;
}

export function useHazardEvents() {
    const [hazardEvents, setHazardEvents] = useState<HazardEvent[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchHazardEvents = async () => {
            try {
                const response = await api.get('/hazard-events');
                setHazardEvents(response.data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to fetch hazard events');
            } finally {
                setLoading(false);
            }
        };

        fetchHazardEvents();
    }, []);

    return { hazardEvents, loading, error };
}