import { useHazardEvents } from '../api/useHazardEvents';

export function HazardEventsList() {
    const { hazardEvents, loading, error } = useHazardEvents();

    if (loading) return <p>Loading hazard events...</p>;
    if (error) return <p style={{ color: 'red' }}>Error: {error}</p>;

    return (
        <div style={{ marginBottom: '2rem' }}>
            <h2>⚠️ Hazard Events</h2>
            {hazardEvents.length === 0 ? (
                <p style={{ color: '#666' }}>No hazard events detected.</p>
            ) : (
                <div>
                    {hazardEvents.map((event) => (
                        <div key={event.id} style={{
                            border: '1px solid #ddd',
                            borderRadius: '8px',
                            padding: '1rem',
                            marginBottom: '1rem',
                            backgroundColor:
                                event.status === 'DANGER' ? '#ffe0e0' :
                                    event.status === 'WATCH' ? '#fff3cd' :
                                        '#e8f5e9'
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                                <div>
                                    <h3 style={{ margin: '0 0 0.5rem 0' }}>{event.eventType}</h3>
                                    <p style={{ margin: '0.25rem 0', fontSize: '0.875rem' }}>
                                        <strong>Time:</strong> {new Date(event.eventTime).toLocaleString()}
                                    </p>
                                    <p style={{ margin: '0.25rem 0', fontSize: '0.875rem' }}>
                                        <strong>Location:</strong> {event.latitude.toFixed(2)}, {event.longitude.toFixed(2)}
                                    </p>
                                    <p style={{ margin: '0.25rem 0', fontSize: '0.875rem' }}>
                                        <strong>Magnitude:</strong> {event.magnitude}
                                    </p>
                                    <p style={{ margin: '0.25rem 0', fontSize: '0.875rem' }}>
                                        <strong>Description:</strong> {event.description}
                                    </p>
                                    {event.deathToll > 0 && (
                                        <p style={{ margin: '0.25rem 0', fontSize: '0.875rem', color: '#d32f2f' }}>
                                            <strong>Casualties:</strong> {event.deathToll}
                                        </p>
                                    )}
                                </div>
                                <span style={{
                                    padding: '0.5rem 1rem',
                                    backgroundColor:
                                        event.status === 'DANGER' ? '#d32f2f' :
                                            event.status === 'WATCH' ? '#f57c00' :
                                                '#388e3c',
                                    color: 'white',
                                    borderRadius: '4px',
                                    fontWeight: 'bold'
                                }}>
                                    {event.status}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
            <p style={{ fontSize: '0.875rem', color: '#666', marginTop: '1rem' }}>
                Total events: {hazardEvents.length}
            </p>
        </div>
    );
}