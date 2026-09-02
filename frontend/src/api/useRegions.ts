import { useState, useEffect } from 'react';
import api from './index';

export interface Region {
    id: number;
    name: string;
    latitude: number;
    longitude: number;
    riskLevel: string;
    population: number;
}

export function useRegions() {
    const [regions, setRegions] = useState<Region[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchRegions = async () => {
            try {
                const response = await api.get('/regions');
                setRegions(response.data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to fetch regions');
            } finally {
                setLoading(false);
            }
        };

        fetchRegions();
    }, []);

    return { regions, loading, error };
}