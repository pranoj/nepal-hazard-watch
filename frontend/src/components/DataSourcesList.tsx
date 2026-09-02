import { useDataSources } from '../api/useDataSources';

export function DataSourcesList() {
    const { dataSources, loading, error } = useDataSources();

    if (loading) return <p>Loading data sources...</p>;
    if (error) return <p style={{ color: 'red' }}>Error: {error}</p>;

    return (
        <div style={{ marginBottom: '2rem' }}>
            <h2>📡 Data Sources</h2>
            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
                gap: '1rem'
            }}>
                {dataSources.map((source) => (
                    <div key={source.id} style={{
                        border: '1px solid #ddd',
                        borderRadius: '8px',
                        padding: '1rem',
                        backgroundColor: '#f9f9f9'
                    }}>
                        <h3 style={{ margin: '0 0 0.5rem 0' }}>{source.name}</h3>
                        <p style={{ margin: '0.25rem 0', fontSize: '0.875rem' }}>
                            <strong>Organization:</strong> {source.organization}
                        </p>
                        <p style={{ margin: '0.25rem 0', fontSize: '0.875rem' }}>
                            <strong>Type:</strong> {source.dataType}
                        </p>
                        <p style={{ margin: '0.25rem 0', fontSize: '0.875rem' }}>
                            <strong>Update Frequency:</strong> {source.updateFrequency}
                        </p>
                        <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.875rem' }}>
                            <span style={{
                                display: 'inline-block',
                                padding: '0.25rem 0.75rem',
                                backgroundColor: source.isOfficial ? '#4c6ef5' : '#748ffc',
                                color: 'white',
                                borderRadius: '4px'
                            }}>
                                {source.isOfficial ? '✓ Official' : 'Research'}
                            </span>
                        </p>
                    </div>
                ))}
            </div>
            <p style={{ fontSize: '0.875rem', color: '#666', marginTop: '1rem' }}>
                Total sources: {dataSources.length}
            </p>
        </div>
    );
}