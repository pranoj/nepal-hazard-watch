import { useState, useEffect } from 'react';
import api from './index';

export interface GlofRiskAssessment {
    id: number;
    sourceType: 'LAKE' | 'GLACIER';
    glacialLakeId: number | null;
    glacierId: number | null;
    lakeName: string;
    icimodId: string;
    riverBasin: string | null;
    latitude: number;
    longitude: number;
    riskScore: number;
    alertLevel: 'NORMAL' | 'WATCH' | 'DANGER' | 'EXTREME';
    rainfallComponent: number;
    earthquakeComponent: number;
    lakeTypeComponent: number;
    seasonalComponent: number;
    landslideComponent: number;
    landslideDetected: boolean;
    landslidePreCondition: boolean;
    rainfallCondition: 'NORMAL' | 'ELEVATED' | 'HEAVY';
    meltCondition: boolean;
    nearestEarthquakeId: number | null;
    assessedAt: string;
}

export function useGlofRiskMap() {
    const [risks, setRisks] = useState<GlofRiskAssessment[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchRisks = async () => {
            try {
                const response = await api.get('/glof/risk-map');
                setRisks(response.data);
                setError(null);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to fetch GLOF risk map');
            } finally {
                setLoading(false);
            }
        };

        fetchRisks();
        const interval = setInterval(fetchRisks, 60000);

        return () => clearInterval(interval);
    }, []);

    return { risks, loading, error };
}
