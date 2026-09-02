import { RegionsList } from './RegionsList';
import { DataSourcesList } from './DataSourcesList';
import { HazardEventsList } from './HazardEventsList';
import { Map } from './Map';
import { useRegions } from '../api/useRegions';
import { useDataSources } from '../api/useDataSources';
import { useHazardEvents } from '../api/useHazardEvents';

export function Dashboard() {
    const { regions } = useRegions();
    const { hazardEvents } = useHazardEvents();
    useDataSources(); // Just load it

    return (
        <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
            <h1>🌏 Nepal Hazard Watch Dashboard</h1>
            <p style={{ color: '#666', marginBottom: '2rem' }}>
                Real-time monitoring of regions, data sources, and hazard events
            </p>

            <Map regions={regions} hazardEvents={hazardEvents} />
            <RegionsList />
            <DataSourcesList />
            <HazardEventsList />
        </div>
    );
}