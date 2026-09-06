import { useEffect, useState } from 'react';
import api from './index';

export interface HazardEvent {
    id: number;
    regionId: number | null;
    eventType: string;
    sourceType: string | null;
    status: string;
    eventTime: string;
    latitude: number;
    longitude: number;
    magnitude: number;
    description: string;
    deathToll: number;
}

export function useHazardEvents() {
    const [events, setEvents] = useState<HazardEvent[]>([]);

    useEffect(() => {
        const fetchEvents = async () => {
            try {
                const response = await api.get('/hazard-events');
                if (Array.isArray(response.data)) {
                    setEvents(response.data);
                }
            } catch {
                // Leave events empty if unavailable.
            }
        };

        fetchEvents();
        const interval = setInterval(fetchEvents, 60_000);
        return () => clearInterval(interval);
    }, []);

    return { events };
}
