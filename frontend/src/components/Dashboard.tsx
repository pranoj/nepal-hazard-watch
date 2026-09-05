import { RegionsList } from './RegionsList';
import { DataSourcesList } from './DataSourcesList';
import { HazardEventsList } from './HazardEventsList';
import { Map } from './Map';
import { AlertStatus } from './AlertStatus';
import { useDataSources } from '../api/useDataSources';
import { useGlofRiskMap } from '../api/useGlofRiskMap';

export function Dashboard() {
    const { risks } = useGlofRiskMap();
    useDataSources(); // Just load it

    return (
        <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
            <h1>🌏 Nepal Hazard Watch Dashboard</h1>
            <p style={{ color: '#666', marginBottom: '2rem' }}>
                Real-time monitoring of regions, data sources, and hazard events
            </p>

            <AlertStatus />

            <Map glofRisks={risks} />
            <RegionsList />
            <DataSourcesList />
            <HazardEventsList />
        </div>
    );
}