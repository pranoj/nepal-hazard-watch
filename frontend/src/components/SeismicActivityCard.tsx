import { useAlertStatus, EarthquakeInfo } from '../api/useAlertStatus';
import { extractNearbyArea, formatNepalTime, formatTimeAgo, minutesAgo } from '../utils/time';
import { glassCardPad, mutedText } from '../utils/theme';

function SeismicEventRow({ event }: { event: EarthquakeInfo }) {
    const isLandslide = event.sourceType === 'landslide';
    return (
        <div style={{ marginBottom: '0.6rem' }}>
            <p style={{ marginBottom: '0.15rem' }}>
                {isLandslide ? '⛰️ Landslide/mass-movement detection' : '🌍 Earthquake'}
                {' — M'}{event.magnitude} near {extractNearbyArea(event.description)}
            </p>
            <p style={{ ...mutedText, fontSize: '0.85rem', marginTop: 0 }}>
                📍 {event.latitude}°N, {event.longitude}°E
                {' · 🕒 '}{formatNepalTime(event.eventTime)}
                {' ('}{formatTimeAgo(minutesAgo(event.eventTime))}{')'}
            </p>
        </div>
    );
}

export function SeismicActivityCard() {
    const { alertStatus, loading } = useAlertStatus();

    return (
        <div style={glassCardPad}>
            <div style={{ fontWeight: 700, marginBottom: '0.5rem' }}>📡 Recent Seismic Activity</div>
            <div style={{ ...mutedText, fontSize: '0.75rem', marginBottom: '0.75rem' }}>Real, from USGS</div>
            {loading && <div style={mutedText}>Loading...</div>}
            {!loading && !alertStatus?.newAlert && <div style={mutedText}>No recent events recorded.</div>}
            {alertStatus?.newAlert && (
                <>
                    <SeismicEventRow event={alertStatus.newAlert} />
                    {alertStatus.lastEarthquake && <SeismicEventRow event={alertStatus.lastEarthquake} />}
                </>
            )}
        </div>
    );
}
