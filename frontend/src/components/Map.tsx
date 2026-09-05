import { MapContainer, TileLayer, Popup, CircleMarker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { useDownstreamTowns } from '../api/useDownstreamTowns';

interface MapProps {
    glofRisks: GlofRiskAssessment[];
}

const ALERT_COLORS: Record<GlofRiskAssessment['alertLevel'], string> = {
    NORMAL: '#16a34a',
    WATCH: '#eab308',
    DANGER: '#ea580c',
    EXTREME: '#dc2626',
};

export function Map({ glofRisks }: MapProps) {
    const center: [number, number] = [28.5, 85.5];
    const { townsByBasin } = useDownstreamTowns();

    return (
        <div style={{ marginTop: '2rem' }}>
            <h2>🗺️ GLOF Risk Map</h2>
            <div style={{ marginBottom: '0.5rem', display: 'flex', gap: '1rem', fontSize: '0.9rem' }}>
                {(Object.keys(ALERT_COLORS) as Array<GlofRiskAssessment['alertLevel']>).map((level) => (
                    <span key={level} style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                        <span style={{
                            width: '10px', height: '10px', borderRadius: '50%',
                            backgroundColor: ALERT_COLORS[level], display: 'inline-block'
                        }} />
                        {level}
                    </span>
                ))}
            </div>
            <MapContainer center={center} zoom={7} style={{ height: '500px', borderRadius: '8px' }}>
                <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

                {glofRisks.map((risk) => {
                    const downstreamTowns = risk.riverBasin ? townsByBasin.get(risk.riverBasin) : undefined;
                    const isGlacier = risk.sourceType === 'GLACIER';
                    return (
                        <CircleMarker
                            key={risk.id}
                            center={[risk.latitude, risk.longitude]}
                            radius={6 + risk.riskScore / 10}
                            pathOptions={{
                                color: ALERT_COLORS[risk.alertLevel],
                                fillColor: ALERT_COLORS[risk.alertLevel],
                                fillOpacity: 0.6,
                            }}
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
                        </CircleMarker>
                    );
                })}
            </MapContainer>
        </div>
    );
}