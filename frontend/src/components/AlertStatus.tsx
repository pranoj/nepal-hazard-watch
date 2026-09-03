import { useAlertStatus } from '../api/useAlertStatus';

function formatTimeAgo(minutes: number): string {
    if (minutes === 0) {
        return `just now`;
    } else if (minutes < 60) {
        return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
    } else if (minutes < 1440) {
        const hours = Math.floor(minutes / 60);
        const remainingMinutes = minutes % 60;
        if (remainingMinutes === 0) {
            return `${hours} hour${hours > 1 ? 's' : ''} ago`;
        } else {
            return `${hours} hour${hours > 1 ? 's' : ''} and ${remainingMinutes} minute${remainingMinutes > 1 ? 's' : ''} ago`;
        }
    } else {
        const days = Math.floor(minutes / 1440);
        return `${days} day${days > 1 ? 's' : ''} ago`;
    }
}

export function AlertStatus() {
    const { alertStatus, loading, error } = useAlertStatus();

    if (loading) return <div>Loading alert status...</div>;
    if (error) return <div style={{ color: 'red' }}>⚠️ {error}</div>;
    if (!alertStatus) return <div>No alert data</div>;

    return (
        <div style={{
            padding: '1.5rem',
            backgroundColor: alertStatus.alertActive ? '#fee2e2' : '#f3f4f6',
            borderRadius: '8px',
            marginBottom: '2rem',
            border: alertStatus.alertActive ? '2px solid #dc2626' : '2px solid #9ca3af'
        }}>
            <h2>🚨 ALERT STATUS</h2>

            <div style={{ marginBottom: '1rem' }}>
                <strong>Active:</strong> {alertStatus.alertActive ? '✅ YES' : '❌ NO'} <br />
                <strong>Time Since Alert:</strong> {formatTimeAgo(alertStatus.minutesSinceAlert)}
            </div>

            <hr />

            <h3>🔴 LATEST EARTHQUAKE</h3>
            <div style={{ marginLeft: '1rem', marginBottom: '1rem' }}>
                <p><strong>Magnitude:</strong> {alertStatus.newAlert.magnitude}</p>
                <p><strong>Location:</strong> {alertStatus.newAlert.latitude}°N, {alertStatus.newAlert.longitude}°E</p>
                <p><strong>Time:</strong> {new Date(alertStatus.newAlert.eventTime).toLocaleString()}</p>
                <p><strong>Description:</strong> {alertStatus.newAlert.description}</p>
                <p><strong>Risk Assessment:</strong> {alertStatus.newAlert.riskAssessment}</p>
            </div>

            {alertStatus.lastEarthquake && (
                <>
                    <hr />
                    <h3>🟡 PREVIOUS EARTHQUAKE</h3>
                    <div style={{ marginLeft: '1rem' }}>
                        <p><strong>Magnitude:</strong> {alertStatus.lastEarthquake.magnitude}</p>
                        <p><strong>Location:</strong> {alertStatus.lastEarthquake.latitude}°N, {alertStatus.lastEarthquake.longitude}°E</p>
                        <p><strong>Time:</strong> {new Date(alertStatus.lastEarthquake.eventTime).toLocaleString()}</p>
                        <p><strong>Description:</strong> {alertStatus.lastEarthquake.description}</p>
                    </div>
                </>
            )}
        </div>
    );
}