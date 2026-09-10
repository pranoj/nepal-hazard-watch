import { useEffect, useState } from 'react';
import api from './index';

export interface Peak {
    key: string;
    name: string;
    latitude: number;
    longitude: number;
    elevationMeters: number;
}

export function usePeaks() {
    const [peaks, setPeaks] = useState<Peak[]>([]);

    useEffect(() => {
        const fetchPeaks = async () => {
            try {
                const response = await api.get('/peaks');
                if (Array.isArray(response.data)) {
                    setPeaks(response.data);
                }
            } catch {
            }
        };

        fetchPeaks();
    }, []);

    return { peaks };
}
