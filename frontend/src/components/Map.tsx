import L from 'leaflet';
import { MapContainer, TileLayer, Popup, Marker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { useDownstreamTowns } from '../api/useDownstreamTowns';
import { useHazardEvents } from '../api/useHazardEvents';
import { alertDivIcon, alertZIndexOffset } from '../utils/alertVisuals';
import { AlertShapeIcon } from './AlertShapeIcon';
import { extractNearbyArea, formatNepalTime, formatTimeAgo, hoursAgo, minutesAgo } from '../utils/time';

interface MapProps {
    glofRisks: GlofRiskAssessment[];
}

const SEISMIC_MARKER_WINDOW_HOURS = 24;

function seismicDivIcon(sourceType: string | null): L.DivIcon {
    const emoji = sourceType === 'landslide' ? '⛰️' : '🌍';
    return L.divIcon({
        html: `<div style="font-size:26px;line-height:1;filter:drop-shadow(0 0 4px rgba(0,0,0,0.6))">${emoji}</div>`,
        className: '',
        iconSize: [30, 30],
        iconAnchor: [15, 15],
        popupAnchor: [0, -15],
    });
}

export function Map({ glofRisks }: MapProps) {
    const center: [number, number] = [28.5, 85.5];
    const { townsByBasin } = useDownstreamTowns();
    const { events } = useHazardEvents();

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
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: 'rgba(244,246,248,0.7)' }}>
                    · 🌍/⛰️ Seismic activity in the last 24h
                </span>
            </div>
            <MapContainer center={center} zoom={7} maxZoom={17} style={{ height: '500px', borderRadius: '8px' }}>
                {/* Sentinel-2 cloudless satellite imagery (EOX), free, no API key.
                    maxNativeZoom caps at real tile resolution (~10m); Leaflet
                    upscales past that instead of requesting missing tiles. */}
                <TileLayer
                    url="https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2020_3857/default/g/{z}/{y}/{x}.jpg"
                    attribution="Sentinel-2 cloudless by <a href=&quot;https://s2maps.eu&quot;>EOX IT Services GmbH</a> (Contains modified Copernicus Sentinel data)"
                    maxNativeZoom={14}
                    maxZoom={17}
                />
                {/* Transparent overlay: city/place labels and borders, so
                    zooming in reveals real place names over the imagery. */}
                <TileLayer
                    url="https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
                    maxZoom={17}
                />

                {glofRisks.map((risk) => {
                    const downstreamTowns = risk.riverBasin ? townsByBasin.get(risk.riverBasin) : undefined;
                    const isGlacier = risk.sourceType === 'GLACIER';
                    return (
                        <Marker
                            key={risk.id}
                            position={[risk.latitude, risk.longitude]}
                            icon={alertDivIcon(risk.alertLevel, risk.riskScore)}
                            zIndexOffset={alertZIndexOffset(risk.alertLevel, risk.riskScore)}
                        >
                            <Popup>
                                <strong>{isGlacier ? '🧊' : '🏔️'} {risk.lakeName}</strong> ({risk.icimodId})<br />
                                {isGlacier && <em>Glacier watch point - no lake yet, steep terminus near a river</em>}<br />
                                GLOF Risk: <strong>{risk.riskScore.toFixed(0)}/100 - {risk.alertLevel}</strong><br />
                                {risk.landslideDetected && (
                                    <span style={{ color: '#dc2626', fontWeight: 'bold' }}>
                                        ⚠️ Landslide/mass-movement detected nearby<br />
                                    </span>
                                )}
                                {risk.landslidePreCondition && (
                                    <span style={{ color: '#ea580c', fontWeight: 'bold' }}>
                                        ⛰️ Steep terrain + heavy rain - elevated landslide pre-condition<br />
                                    </span>
                                )}
                                {risk.rainfallCondition === 'HEAVY' && (
                                    <span style={{ color: '#dc2626', fontWeight: 'bold' }}>
                                        🌧️ Heavy rainfall condition<br />
                                    </span>
                                )}
                                {risk.meltCondition && (
                                    <span>🌡️ Active melt conditions<br /></span>
                                )}
                                Rainfall factor: {(risk.rainfallComponent * 100).toFixed(0)}% ({risk.rainfallCondition})<br />
                                Earthquake factor: {(risk.earthquakeComponent * 100).toFixed(0)}%<br />
                                Landslide factor: {(risk.landslideComponent * 100).toFixed(0)}%<br />
                                {isGlacier ? 'Terrain steepness' : 'Lake type'} factor: {(risk.lakeTypeComponent * 100).toFixed(0)}%<br />
                                Season factor: {(risk.seasonalComponent * 100).toFixed(0)}%<br />
                                {downstreamTowns && downstreamTowns.length > 0 && (
                                    <>
                                        🏘️ Downstream ({risk.riverBasin}): {downstreamTowns.map(t => t.townName).join(' → ')}<br />
                                    </>
                                )}
                                <small>Assessed: {new Date(risk.assessedAt).toLocaleString()}</small>
                            </Popup>
                        </Marker>
                    );
                })}

                {recentSeismicEvents.map((event) => (
                    <Marker
                        key={`seismic-${event.id}`}
                        position={[event.latitude, event.longitude]}
                        icon={seismicDivIcon(event.sourceType)}
                        zIndexOffset={5000}
                    >
                        <Popup>
                            <strong>
                                {event.sourceType === 'landslide' ? '⛰️ Landslide/mass-movement' : '🌍 Earthquake'}
                                {' — M'}{event.magnitude}
                            </strong><br />
                            {extractNearbyArea(event.description)}<br />
                            📍 {event.latitude}°N, {event.longitude}°E<br />
                            🕒 {formatNepalTime(event.eventTime)} ({formatTimeAgo(minutesAgo(event.eventTime))})
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}