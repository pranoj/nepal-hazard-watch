import { useRegions } from '../api/useRegions';

export function RegionsList() {
    const { regions, loading, error } = useRegions();

    if (loading) return <p>Loading regions...</p>;
    if (error) return <p style={{ color: 'red' }}>Error: {error}</p>;

    return (
        <div style={{ marginBottom: '2rem' }}>
            <h2>📍 Regions at Risk</h2>
            <table style={{
                width: '100%',
                borderCollapse: 'collapse',
                border: '1px solid #ddd'
            }}>
                <thead>
                    <tr style={{ backgroundColor: '#f0f0f0' }}>
                        <th style={{ padding: '0.5rem', textAlign: 'left', border: '1px solid #ddd' }}>Name</th>
                        <th style={{ padding: '0.5rem', textAlign: 'left', border: '1px solid #ddd' }}>Risk Level</th>
                        <th style={{ padding: '0.5rem', textAlign: 'left', border: '1px solid #ddd' }}>Population</th>
                        <th style={{ padding: '0.5rem', textAlign: 'left', border: '1px solid #ddd' }}>Coordinates</th>
                    </tr>
                </thead>
                <tbody>
                    {regions.map((region) => (
                        <tr key={region.id} style={{ borderBottom: '1px solid #ddd' }}>
                            <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>{region.name}</td>
                            <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>
                                <span style={{
                                    padding: '0.25rem 0.5rem',
                                    backgroundColor:
                                        region.riskLevel === 'HIGH' ? '#ff6b6b' :
                                            region.riskLevel === 'MEDIUM' ? '#ffa94d' :
                                                '#51cf66',
                                    color: 'white',
                                    borderRadius: '4px',
                                    fontSize: '0.875rem'
                                }}>
                                    {region.riskLevel}
                                </span>
                            </td>
                            <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>{region.population.toLocaleString()}</td>
                            <td style={{ padding: '0.5rem', border: '1px solid #ddd', fontSize: '0.875rem' }}>
                                {region.latitude.toFixed(2)}, {region.longitude.toFixed(2)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <p style={{ fontSize: '0.875rem', color: '#666', marginTop: '0.5rem' }}>
                Total regions: {regions.length}
            </p>
        </div>
    );
}