import { Radio, Activity, TriangleAlert, MapPin, Clock } from 'lucide-react';
import { useAlertStatus, EarthquakeInfo } from '../api/useAlertStatus';
import { extractNearbyArea, formatNepalTime, formatTimeAgo, minutesAgo } from '../utils/time';
import { glassCardPad, mutedText } from '../utils/theme';

function SeismicEventRow({ event }: { event: EarthquakeInfo }) {
    const isLandslide = event.sourceType === 'landslide';
    return (
        <div style={{ marginBottom: '0.6rem' }}>
            <p style={{ marginBottom: '0.2rem', display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
                {isLandslide
                    ? <TriangleAlert size={21} strokeWidth={2.1} color="#f97316" style={{ flexShrink: 0 }} />
                    : <Activity size={21} strokeWidth={2.1} color="#60a5fa" style={{ flexShrink: 0 }} />}
                {isLandslide ? 'Landslide/mass-movement detection' : 'Earthquake'}
                {' · M'}{event.magnitude} near {extractNearbyArea(event.description)}
            </p>
            <p style={{ ...mutedText, fontSize: '0.85rem', marginTop: 0, display: 'flex', alignItems: 'center', gap: '5px', flexWrap: 'wrap' }}>
                <MapPin size={13} strokeWidth={2} /> {event.latitude}°N, {event.longitude}°E
                <span>·</span>
                <Clock size={13} strokeWidth={2} /> {formatNepalTime(event.eventTime)}
                {' ('}{formatTimeAgo(minutesAgo(event.eventTime))}{')'}
            </p>
        </div>
    );
}

export function SeismicActivityCard() {
    const { alertStatus, loading } = useAlertStatus();

    return (
        <div style={glassCardPad}>
            <div style={{ fontWeight: 700, fontSize: '1.05rem', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                <Radio size={19} strokeWidth={2} /> Recent Seismic Activity
            </div>
            <div style={{ ...mutedText, fontSize: '0.75rem', marginBottom: '0.75rem' }}>Data from USGS</div>
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
