import { Snowflake, Mountain, TriangleAlert, ShieldAlert, CloudRain, Thermometer, Building2, Satellite } from 'lucide-react';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { RiverBasinTown } from '../api/useDownstreamTowns';

interface RiskDetailContentProps {
    risk: GlofRiskAssessment;
    downstreamTowns?: RiverBasinTown[];
}

// Shared with the map's own marker popup (see Map.tsx), so clicking a name
// anywhere in the app shows exactly the same card as clicking its point on
// the map, not a second, differently-worded summary.
export function RiskDetailContent({ risk, downstreamTowns }: RiskDetailContentProps) {
    const isGlacier = risk.sourceType === 'GLACIER';
    const satelliteFlagLabel = isGlacier
        ? 'Satellite: sudden ice-cover drop detected'
        : 'Satellite: lake growth detected';
    const satelliteFlagActive = isGlacier ? risk.satelliteIceSuddenDropDetected : risk.satelliteLakeGrowthDetected;

    return (
        <div>
            <strong style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                {isGlacier ? <Snowflake size={14} strokeWidth={2} /> : <Mountain size={14} strokeWidth={2} />}
                {risk.lakeName}
            </strong> ({risk.icimodId})<br />
            {isGlacier && (
                <>
                    <em>Glacier watch point - no lake yet, steep terminus near a river</em><br />
                </>
            )}
            GLOF Risk: <strong>{risk.riskScore.toFixed(0)}/100 - {risk.alertLevel}</strong><br />
            {risk.landslideDetected && (
                <>
                    <span style={{ color: '#dc2626', fontWeight: 'bold', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <TriangleAlert size={14} strokeWidth={2} /> Landslide/mass-movement detected nearby
                    </span><br />
                </>
            )}
            {risk.landslidePreCondition && (
                <>
                    <span style={{ color: '#ea580c', fontWeight: 'bold', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <ShieldAlert size={14} strokeWidth={2} /> Steep terrain + heavy rain - elevated landslide pre-condition
                    </span><br />
                </>
            )}
            {risk.rainfallCondition === 'HEAVY' && (
                <>
                    <span style={{ color: '#dc2626', fontWeight: 'bold', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <CloudRain size={14} strokeWidth={2} /> Heavy rainfall condition
                    </span><br />
                </>
            )}
            {risk.meltCondition && (
                <>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <Thermometer size={14} strokeWidth={2} /> Active melt conditions
                    </span><br />
                </>
            )}
            {satelliteFlagActive && (
                <>
                    <span style={{ color: '#dc2626', fontWeight: 'bold', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <Satellite size={14} strokeWidth={2} /> {satelliteFlagLabel}
                    </span><br />
                </>
            )}
            Rainfall factor: {(risk.rainfallComponent * 100).toFixed(0)}% ({risk.rainfallCondition})<br />
            Earthquake factor: {(risk.earthquakeComponent * 100).toFixed(0)}%<br />
            Landslide factor: {(risk.landslideComponent * 100).toFixed(0)}%<br />
            {isGlacier ? 'Terrain steepness' : 'Lake type'} factor: {(risk.lakeTypeComponent * 100).toFixed(0)}%<br />
            Season factor: {(risk.seasonalComponent * 100).toFixed(0)}%<br />
            {risk.satelliteComponent != null && (
                <>Satellite factor: {(risk.satelliteComponent * 100).toFixed(0)}%<br /></>
            )}
            {downstreamTowns && downstreamTowns.length > 0 && (
                <>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <Building2 size={14} strokeWidth={2} /> Downstream ({risk.riverBasin}): {downstreamTowns.map(t => t.townName).join(' → ')}
                    </span><br />
                </>
            )}
            <small>Assessed: {new Date(risk.assessedAt).toLocaleString()}</small>
        </div>
    );
}
