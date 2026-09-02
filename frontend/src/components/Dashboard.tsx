import { RegionsList } from './RegionsList';
import { DataSourcesList } from './DataSourcesList';
import { HazardEventsList } from './HazardEventsList';

export function Dashboard() {
    return (
        <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
            <h1>🌏 Nepal Hazard Watch Dashboard</h1>
            <p style={{ color: '#666', marginBottom: '2rem' }}>
                Real-time monitoring of regions, data sources, and hazard events
            </p>

            <RegionsList />
            <DataSourcesList />
            <HazardEventsList />
        </div>
    );
}