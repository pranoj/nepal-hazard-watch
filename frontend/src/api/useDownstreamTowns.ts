import { useEffect, useState } from 'react';
import api from './index';

export interface RiverBasinTown {
    id: number;
    riverBasin: string;
    townName: string;
    latitude: number;
    longitude: number;
    downstreamOrder: number;
}

export function useDownstreamTowns() {
    const [townsByBasin, setTownsByBasin] = useState<Map<string, RiverBasinTown[]>>(new Map());
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchTowns = async () => {
            try {
                const response = await api.get('/glof/downstream-towns');
                const towns: RiverBasinTown[] = response.data;
                const grouped = new Map<string, RiverBasinTown[]>();
                for (const town of towns) {
                    const list = grouped.get(town.riverBasin) ?? [];
                    list.push(town);
                    grouped.set(town.riverBasin, list);
                }
                for (const list of grouped.values()) {
                    list.sort((a, b) => a.downstreamOrder - b.downstreamOrder);
                }
                setTownsByBasin(grouped);
            } catch {
                // Downstream-town context is supplementary; a fetch failure
                // shouldn't block the rest of the risk display.
            } finally {
                setLoading(false);
            }
        };

        fetchTowns();
    }, []);

    return { townsByBasin, loading };
}
