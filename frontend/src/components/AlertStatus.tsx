import { useAlertStatus } from '../api/useAlertStatus';
import { useGlofRiskMap, GlofRiskAssessment } from '../api/useGlofRiskMap';
import { useDownstreamTowns, RiverBasinTown } from '../api/useDownstreamTowns';

const ALERT_COLORS: Record<GlofRiskAssessment['alertLevel'], string> = {
    NORMAL: '#16a34a',
    WATCH: '#eab308',
    DANGER: '#ea580c',
    EXTREME: '#dc2626',
};

const MAX_POINTS_PER_BASIN = 4;

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

function dominantFactor(risk: GlofRiskAssessment): string {
    const isGlacier = risk.sourceType === 'GLACIER';
    const factors: Array<[string, number]> = [
        ['heavy rainfall', risk.rainfallComponent],
        ['recent nearby earthquake', risk.earthquakeComponent],
        ['recent landslide/mass-movement', risk.landslideComponent],
        [isGlacier ? 'steep terrain' : 'lake type/history', risk.lakeTypeComponent],
        ['monsoon season', risk.seasonalComponent],
    ];
    factors.sort((a, b) => b[1] - a[1]);
    return factors[0][0];
}

function groupByBasin(risks: GlofRiskAssessment[]): Array<[string, GlofRiskAssessment[]]> {
    const groups = new Map<string, GlofRiskAssessment[]>();
    for (const risk of risks) {
        const key = risk.riverBasin ?? 'Other';
        const list = groups.get(key) ?? [];
        list.push(risk);
        groups.set(key, list);
    }
    for (const list of groups.values()) {
        list.sort((a, b) => b.riskScore - a.riskScore);
    }
    return [...groups.entries()].sort((a, b) => b[1].length - a[1].length);
}

function PointRow({ risk }: { risk: GlofRiskAssessment }) {
    const isGlacier = risk.sourceType === 'GLACIER';

    return (
        <div style={{ padding: '0.35rem 0', borderBottom: '1px solid rgba(0,0,0,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>
                    <span style={{
                        display: 'inline-block', width: '10px', height: '10px', borderRadius: '50%',
                        backgroundColor: ALERT_COLORS[risk.alertLevel], marginRight: '0.5rem'
                    }} />
                    {isGlacier ? '🧊' : '🏔️'} <strong>{risk.lakeName}</strong> - driven by {dominantFactor(risk)}
                </span>
                <span style={{ fontWeight: 'bold', color: ALERT_COLORS[risk.alertLevel] }}>
                    {risk.riskScore.toFixed(0)}/100
                </span>
            </div>
            {(risk.landslideDetected || risk.rainfallCondition === 'HEAVY' || risk.meltCondition) && (
                <div style={{ marginLeft: '1.3rem', fontSize: '0.8rem', color: '#dc2626' }}>
                    {risk.landslideDetected && <span>⚠️ Landslide/mass-movement detected nearby&nbsp;&nbsp;</span>}
                    {risk.rainfallCondition === 'HEAVY' && <span>🌧️ Heavy rainfall&nbsp;&nbsp;</span>}
                    {risk.meltCondition && <span style={{ color: '#666' }}>🌡️ Active melt</span>}
                </div>
            )}
        </div>
    );
}

function BasinGroup({ basin, points, townsByBasin }: {
    basin: string; points: GlofRiskAssessment[]; townsByBasin: Map<string, RiverBasinTown[]>;
}) {
    const downstreamTowns = townsByBasin.get(basin);
    const shown = points.slice(0, MAX_POINTS_PER_BASIN);
    const hidden = points.length - shown.length;

    return (
        <div style={{ marginBottom: '1rem' }}>
            <div style={{ fontWeight: 'bold', fontSize: '0.9rem' }}>
                {basin} - {points.length} point{points.length > 1 ? 's' : ''}
            </div>
            {downstreamTowns && downstreamTowns.length > 0 && (
                <div style={{ fontSize: '0.85rem', color: '#555', marginBottom: '0.3rem' }}>
                    🏘️ Downstream: {downstreamTowns.map(t => t.townName).join(' → ')}
                </div>
            )}
            {shown.map(risk => <PointRow key={risk.id} risk={risk} />)}
            {hidden > 0 && (
                <p style={{ color: '#666', fontSize: '0.85rem', margin: '0.3rem 0 0' }}>
                    + {hidden} more in {basin} - see the map below for the full list
                </p>
            )}
        </div>
    );
}

export function AlertStatus() {
    const { risks, loading: risksLoading, error: risksError } = useGlofRiskMap();
    const { alertStatus } = useAlertStatus();
    const { townsByBasin } = useDownstreamTowns();

    if (risksLoading) return <div>Loading GLOF risk status...</div>;
    if (risksError) return <div style={{ color: 'red' }}>⚠️ {risksError}</div>;

    const critical = risks.filter(r => r.alertLevel === 'DANGER' || r.alertLevel === 'EXTREME');
    const watch = risks.filter(r => r.alertLevel === 'WATCH');
    const hasCritical = critical.length > 0;
    const hasWatch = watch.length > 0;

    const bannerColor = hasCritical ? '#dc2626' : hasWatch ? '#eab308' : '#16a34a';
    const bannerBg = hasCritical ? '#fee2e2' : hasWatch ? '#fefce8' : '#f0fdf4';
    const headline = hasCritical
        ? `🚨 ${critical.length} point${critical.length > 1 ? 's' : ''} at DANGER/EXTREME risk`
        : hasWatch
            ? `🟡 ${watch.length} point${watch.length > 1 ? 's' : ''} under WATCH`
            : '✅ All monitored points NORMAL';

    return (
        <div style={{
            padding: '1.5rem',
            backgroundColor: bannerBg,
            borderRadius: '8px',
            marginBottom: '2rem',
            border: `2px solid ${bannerColor}`
        }}>
            <h2 style={{ margin: 0 }}>{headline}</h2>
            <p style={{ color: '#555', marginTop: '0.25rem' }}>
                {risks.length} points monitored across Nepal (glacial lakes + glacier watch points)
            </p>

            {hasCritical && (
                <>
                    <hr />
                    <h3>🚨 Points requiring attention</h3>
                    {groupByBasin(critical).map(([basin, points]) => (
                        <BasinGroup key={basin} basin={basin} points={points} townsByBasin={townsByBasin} />
                    ))}
                </>
            )}

            {!hasCritical && hasWatch && (
                <>
                    <hr />
                    <h3>🟡 Points under enhanced monitoring</h3>
                    {groupByBasin(watch).map(([basin, points]) => (
                        <BasinGroup key={basin} basin={basin} points={points} townsByBasin={townsByBasin} />
                    ))}
                </>
            )}

            {alertStatus?.newAlert && (
                <>
                    <hr />
                    <h3 style={{ fontSize: '1rem', color: '#555' }}>Most recent seismic activity</h3>
                    <p style={{ color: '#555' }}>
                        M{alertStatus.newAlert.magnitude} - {alertStatus.newAlert.latitude}°N, {alertStatus.newAlert.longitude}°E
                        {' - '}{formatTimeAgo(alertStatus.minutesSinceAlert)}
                    </p>
                </>
            )}
        </div>
    );
}
