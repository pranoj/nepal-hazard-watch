import { useState, useEffect } from 'react';
import api from './index';

export interface DataSource {
    id: number;
    name: string;
    organization: string;
    dataType: string;
    isOfficial: boolean;
    updateFrequency: string;
}

export function useDataSources() {
    const [dataSources, setDataSources] = useState<DataSource[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchDataSources = async () => {
            try {
                const response = await api.get('/data-sources');
                setDataSources(response.data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to fetch data sources');
            } finally {
                setLoading(false);
            }
        };

        fetchDataSources();
    }, []);

    return { dataSources, loading, error };
}