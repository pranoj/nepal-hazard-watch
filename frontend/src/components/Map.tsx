import { useEffect, useRef } from 'react';
import L from 'leaflet';
import { renderToStaticMarkup } from 'react-dom/server';
import { MapContainer, TileLayer, Popup, Marker, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Activity, TriangleAlert, MapPin, Clock, Mountain } from 'lucide-react';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { useDownstreamTowns } from '../api/useDownstreamTowns';
import { useHazardEvents } from '../api/useHazardEvents';
import { usePeaks } from '../api/usePeaks';
import { alertDivIcon, alertZIndexOffset } from '../utils/alertVisuals';
import { AlertShapeIcon } from './AlertShapeIcon';
import { RiskDetailContent } from './RiskDetailContent';
import { extractNearbyArea, formatNepalTime, formatTimeAgo, hoursAgo, minutesAgo } from '../utils/time';

// `token` changes on every click so the focus effect always re-fires. Exactly one of riskId/seismic/peak is set.
export interface MapFocusTarget {
    token: number;
    riskId?: number;
    seismic?: { id: number; latitude: number; longitude: number };
    peak?: { key: string; latitude: number; longitude: number };
}

interface MapProps {
    glofRisks: GlofRiskAssessment[];
    focusTarget?: MapFocusTarget | null;
    onSelectRisk?: (riskId: number) => void;
    onSelectPeak?: (peakKey: string) => void;
}

// lives inside MapContainer (useMap only works below it) - pans to the requested site and opens its real marker popup
function FocusHandler({ focusTarget, glofRisks, markerRefs, seismicMarkerRefs, peakMarkerRefs }: {
    focusTarget: MapFocusTarget | null | undefined;
    glofRisks: GlofRiskAssessment[];
    markerRefs: React.MutableRefObject<globalThis.Map<number, L.Marker>>;
    seismicMarkerRefs: React.MutableRefObject<globalThis.Map<number, L.Marker>>;
    peakMarkerRefs: React.MutableRefObject<globalThis.Map<string, L.Marker>>;
}) {
    const map = useMap();

    useEffect(() => {
        if (!focusTarget) return;

        if (focusTarget.seismic) {
            const { id, latitude, longitude } = focusTarget.seismic;
            map.flyTo([latitude, longitude], Math.max(map.getZoom(), 12), { duration: 0.8 });
            seismicMarkerRefs.current.get(id)?.openPopup();
            return;
        }

        if (focusTarget.peak) {
            const { key, latitude, longitude } = focusTarget.peak;
            map.flyTo([latitude, longitude], Math.max(map.getZoom(), 12), { duration: 0.8 });
            peakMarkerRefs.current.get(key)?.openPopup();
            return;
        }

        const risk = glofRisks.find(r => r.id === focusTarget.riskId);
        if (!risk) return;

        map.flyTo([risk.latitude, risk.longitude], Math.max(map.getZoom(), 12), { duration: 0.8 });
        const marker = markerRefs.current.get(focusTarget.riskId!);
        marker?.openPopup();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [focusTarget]);

    return null;
}

const SEISMIC_MARKER_WINDOW_HOURS = 24;

function seismicDivIcon(sourceType: string | null): L.DivIcon {
    const isLandslide = sourceType === 'landslide';
    const Icon = isLandslide ? TriangleAlert : Activity;
    // earthquake was near-invisible at the old light gray (#e2e8f0) - red matches the "critical" red used elsewhere (EXTREME banner)
    const color = isLandslide ? '#f97316' : '#dc2626';
    const size = 34;
    // static "vibrating" look: two blurred offset copies behind a sharp one, comic-panel motion blur, nothing actually animates
    const svg = renderToStaticMarkup(
        <div style={{ position: 'relative', width: size + 8, height: size + 8 }}>
            <div style={{ position: 'absolute', top: -3, left: -3, opacity: 0.4, filter: 'blur(2px)' }}>
                <Icon size={size} color={color} strokeWidth={2.25} />
            </div>
            <div style={{ position: 'absolute', top: 3, left: 3, opacity: 0.4, filter: 'blur(2px)' }}>
                <Icon size={size} color={color} strokeWidth={2.25} />
            </div>
            <div style={{ position: 'absolute', top: 0, left: 0, filter: 'drop-shadow(0 0 3px rgba(0,0,0,0.85))' }}>
                <Icon size={size} color={color} strokeWidth={2.25} />
            </div>
        </div>,
    );
    return L.divIcon({
        html: svg,
        className: '',
        iconSize: [size + 8, size + 8],
        iconAnchor: [(size + 8) / 2, (size + 8) / 2],
        popupAnchor: [0, -(size + 8) / 2],
    });
}

const peakIcon = L.divIcon({
    html: renderToStaticMarkup(
        <div style={{ filter: 'drop-shadow(0 0 3px rgba(0,0,0,0.85))' }}>
            <Mountain size={22} color="#e2e8f0" strokeWidth={2} />
        </div>,
    ),
    className: '',
    iconSize: [22, 22],
    iconAnchor: [11, 11],
    popupAnchor: [0, -11],
});

export function Map({ glofRisks, focusTarget, onSelectRisk, onSelectPeak }: MapProps) {
    const center: [number, number] = [28.5, 85.5];
    const { townsByBasin } = useDownstreamTowns();
    const { events } = useHazardEvents();
    const { peaks } = usePeaks();
    const markerRefs = useRef<globalThis.Map<number, L.Marker>>(new globalThis.Map());
    const seismicMarkerRefs = useRef<globalThis.Map<number, L.Marker>>(new globalThis.Map());
    const peakMarkerRefs = useRef<globalThis.Map<string, L.Marker>>(new globalThis.Map());

    const recentSeismicEvents = events.filter(
        (e) => e.eventType === 'EARTHQUAKE' && hoursAgo(e.eventTime) < SEISMIC_MARKER_WINDOW_HOURS,
    );

    return (
        <div>
            <div style={{ marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.8rem', color: '#f4f6f8', flexWrap: 'wrap' }}>
                <span style={{ color: 'rgba(244,246,248,0.55)' }}>Alert levels:</span>
                {(['NORMAL', 'WATCH', 'DANGER', 'EXTREME'] as const).map((level) => (
                    <span key={level} style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                        <AlertShapeIcon level={level} size={14} />
                        {level}
                    </span>
                ))}
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: 'rgba(244,246,248,0.7)' }}>
                    · <Activity size={13} strokeWidth={2} />/<TriangleAlert size={13} strokeWidth={2} color="#f97316" /> Seismic activity in the last 24h
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: 'rgba(244,246,248,0.7)' }}>
                    · <Mountain size={13} strokeWidth={2} /> Nepal's 16 highest peaks (informational, not risk-monitored)
                </span>
            </div>
            <MapContainer center={center} zoom={7} maxZoom={17} style={{ height: '500px', borderRadius: '8px' }}>
                {/* Sentinel-2 cloudless imagery (EOX); maxNativeZoom caps at real tile resolution (~10m), Leaflet upscales past that */}
                <TileLayer
                    url="https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2020_3857/default/g/{z}/{y}/{x}.jpg"
                    attribution="Sentinel-2 cloudless by <a href=&quot;https://s2maps.eu&quot;>EOX IT Services GmbH</a> (Contains modified Copernicus Sentinel data)"
                    maxNativeZoom={14}
                    maxZoom={17}
                />
                {/* transparent overlay: place labels/borders over the imagery */}
                <TileLayer
                    url="https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
                    attribution="Labels &copy; <a href=&quot;https://www.esri.com/&quot;>Esri</a>"
                    maxZoom={17}
                />

                <FocusHandler focusTarget={focusTarget} glofRisks={glofRisks} markerRefs={markerRefs} seismicMarkerRefs={seismicMarkerRefs} peakMarkerRefs={peakMarkerRefs} />

                {glofRisks.map((risk) => {
                    const downstreamTowns = risk.riverBasin ? townsByBasin.get(risk.riverBasin) : undefined;
                    return (
                        <Marker
                            key={risk.id}
                            ref={(el) => {
                                if (el) markerRefs.current.set(risk.id, el);
                                else markerRefs.current.delete(risk.id);
                            }}
                            position={[risk.latitude, risk.longitude]}
                            icon={alertDivIcon(risk.alertLevel, risk.riskScore)}
                            zIndexOffset={alertZIndexOffset(risk.alertLevel, risk.riskScore)}
                            eventHandlers={{ click: () => onSelectRisk?.(risk.id) }}
                        >
                            <Popup>
                                <RiskDetailContent risk={risk} downstreamTowns={downstreamTowns} />
                            </Popup>
                        </Marker>
                    );
                })}

                {recentSeismicEvents.map((event) => (
                    <Marker
                        key={`seismic-${event.id}`}
                        ref={(el) => {
                            if (el) seismicMarkerRefs.current.set(event.id, el);
                            else seismicMarkerRefs.current.delete(event.id);
                        }}
                        position={[event.latitude, event.longitude]}
                        icon={seismicDivIcon(event.sourceType)}
                        zIndexOffset={5000}
                    >
                        <Popup>
                            <strong style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                {event.sourceType === 'landslide'
                                    ? <TriangleAlert size={14} strokeWidth={2} />
                                    : <Activity size={14} strokeWidth={2} />}
                                {event.sourceType === 'landslide' ? 'Landslide/mass-movement' : 'Earthquake'}
                                {' · M'}{event.magnitude}
                            </strong><br />
                            {extractNearbyArea(event.description)}<br />
                            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                <MapPin size={13} strokeWidth={2} /> {event.latitude}°N, {event.longitude}°E
                            </span><br />
                            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                <Clock size={13} strokeWidth={2} /> {formatNepalTime(event.eventTime)} ({formatTimeAgo(minutesAgo(event.eventTime))})
                            </span>
                        </Popup>
                    </Marker>
                ))}

                {peaks.map((peak) => (
                    <Marker
                        key={peak.key}
                        ref={(el) => {
                            if (el) peakMarkerRefs.current.set(peak.key, el);
                            else peakMarkerRefs.current.delete(peak.key);
                        }}
                        position={[peak.latitude, peak.longitude]}
                        icon={peakIcon}
                        eventHandlers={{ click: () => onSelectPeak?.(peak.key) }}
                    >
                        <Popup>
                            <strong style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                <Mountain size={14} strokeWidth={2} /> {peak.name}
                            </strong><br />
                            {peak.elevationMeters.toLocaleString()} m
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}