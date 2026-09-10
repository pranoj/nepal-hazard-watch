import { Mountain, Snowflake, TriangleAlert, ShieldAlert, CloudRain, Thermometer, Building2, Info } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { useGlofRiskMap, GlofRiskAssessment } from '../api/useGlofRiskMap';
import { useDownstreamTowns, RiverBasinTown } from '../api/useDownstreamTowns';
import { ALERT_COLORS } from '../utils/alertVisuals';
import { AlertShapeIcon } from './AlertShapeIcon';
import { glassCard, mutedText } from '../utils/theme';
import { hasActiveTrigger } from '../utils/risk';

const MAX_POINTS_PER_BASIN = 4;

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

// "1 Lake", "2 Glaciers", or "100 Lakes and 2 Glaciers" instead of generic GIS-jargon "point"
function describeCounts(points: GlofRiskAssessment[]): string {
    const lakes = points.filter(p => p.sourceType !== 'GLACIER').length;
    const glaciers = points.filter(p => p.sourceType === 'GLACIER').length;
    const parts: string[] = [];
    if (lakes > 0) parts.push(`${lakes} Lake${lakes > 1 ? 's' : ''}`);
    if (glaciers > 0) parts.push(`${glaciers} Glacier${glaciers > 1 ? 's' : ''}`);
    return parts.join(' and ');
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

// opens the same card clicking its point on the map would show
function ClickableName({ risk, onSelect }: { risk: GlofRiskAssessment; onSelect: (risk: GlofRiskAssessment) => void }) {
    return (
        <span
            onClick={() => onSelect(risk)}
            style={{ cursor: 'pointer', textDecoration: 'underline', textDecorationStyle: 'dotted', textUnderlineOffset: '2px' }}
        >
            {risk.lakeName}
        </span>
    );
}

function ClickableNameList({ risks, onSelect }: { risks: GlofRiskAssessment[]; onSelect: (risk: GlofRiskAssessment) => void }) {
    return (
        <>
            {risks.map((r, i) => (
                <span key={r.id}>
                    {i > 0 && ', '}
                    <ClickableName risk={r} onSelect={onSelect} />
                </span>
            ))}
        </>
    );
}

interface RiskFlag { Icon: LucideIcon; label: string }

function riskFlags(risk: GlofRiskAssessment): RiskFlag[] {
    const flags: RiskFlag[] = [];
    if (risk.landslideDetected) flags.push({ Icon: TriangleAlert, label: 'landslide detected' });
    if (risk.landslidePreCondition) flags.push({ Icon: ShieldAlert, label: 'steep + wet' });
    if (risk.rainfallCondition === 'HEAVY') flags.push({ Icon: CloudRain, label: 'heavy rain' });
    if (risk.meltCondition) flags.push({ Icon: Thermometer, label: 'melt' });
    return flags;
}

function PointRow({ risk, onSelect }: { risk: GlofRiskAssessment; onSelect: (risk: GlofRiskAssessment) => void }) {
    const isGlacier = risk.sourceType === 'GLACIER';
    const TypeIcon = isGlacier ? Snowflake : Mountain;
    const flags = riskFlags(risk);

    return (
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', padding: '0.15rem 0', fontSize: '0.85rem' }}>
            <AlertShapeIcon level={risk.alertLevel} size={10} />
            <span style={{
                flex: 1, minWidth: 0,
                display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '0.35rem',
            }}>
                <TypeIcon size={13} strokeWidth={2} style={{ flexShrink: 0, color: 'rgba(244,246,248,0.75)' }} />
                <span style={{ overflowWrap: 'break-word' }}>
                    <strong><ClickableName risk={risk} onSelect={onSelect} /></strong>
                    <span style={mutedText}> · {dominantFactor(risk)}</span>
                </span>
                {flags.map(({ Icon, label }, i) => (
                    <span key={i} style={{ ...mutedText, display: 'inline-flex', alignItems: 'center', gap: '3px', flexShrink: 0 }}>
                        <Icon size={12} strokeWidth={2} /> {label}
                    </span>
                ))}
            </span>
            <span style={{ flexShrink: 0, display: 'flex', alignItems: 'baseline', gap: '2px' }}>
                <span style={{ fontWeight: 800, fontSize: '1.1rem', color: ALERT_COLORS[risk.alertLevel] }}>
                    {risk.riskScore.toFixed(0)}
                </span>
                <span style={{ ...mutedText, fontSize: '0.65rem' }}>/100</span>
            </span>
        </div>
    );
}

function BasinGroup({ basin, points, townsByBasin, onSelect }: {
    basin: string; points: GlofRiskAssessment[]; townsByBasin: Map<string, RiverBasinTown[]>;
    onSelect: (risk: GlofRiskAssessment) => void;
}) {
    const downstreamTowns = townsByBasin.get(basin);
    const shown = points.slice(0, MAX_POINTS_PER_BASIN);
    const hidden = points.length - shown.length;

    return (
        <div style={{ padding: '0.3rem 0', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>
                    {basin} <span style={mutedText}>({points.length})</span>
                </div>
                {downstreamTowns && downstreamTowns.length > 0 && (
                    <div style={{
                        ...mutedText, fontSize: '0.72rem',
                        display: 'flex', alignItems: 'flex-start', gap: '0.3rem',
                    }}>
                        <Building2 size={12} strokeWidth={2} style={{ flexShrink: 0, marginTop: '0.15rem' }} />
                        <span style={{ overflowWrap: 'break-word' }}>
                            <strong>Downstream:</strong> {downstreamTowns.map(t => t.townName).join(' → ')}
                        </span>
                    </div>
                )}
            </div>
            {shown.map(risk => <PointRow key={risk.id} risk={risk} onSelect={onSelect} />)}
            {hidden > 0 && (
                <p style={{ ...mutedText, fontSize: '0.75rem', margin: '0.2rem 0 0' }}>
                    + {hidden} more · see map
                </p>
            )}
        </div>
    );
}

interface AlertStatusProps {
    onSelectRisk: (risk: GlofRiskAssessment) => void;
}

export function AlertStatus({ onSelectRisk }: AlertStatusProps) {
    const { risks, loading: risksLoading, error: risksError } = useGlofRiskMap();
    const { townsByBasin } = useDownstreamTowns();

    if (risksLoading) return <div style={{ ...glassCard, padding: '1rem 1.25rem' }}>Loading GLOF risk status...</div>;
    if (risksError) return (
        <div style={{ ...glassCard, padding: '1rem 1.25rem', color: '#f87171', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <TriangleAlert size={16} strokeWidth={2} /> {risksError}
        </div>
    );

    // DANGER/EXTREME always means a real trigger (static factors alone cap at 30) - WATCH needs splitting on hasActiveTrigger
    const critical = risks.filter(r => r.alertLevel === 'DANGER' || r.alertLevel === 'EXTREME');
    const watchTriggered = risks.filter(r => r.alertLevel === 'WATCH' && hasActiveTrigger(r));
    const watchBaseline = risks.filter(r => r.alertLevel === 'WATCH' && !hasActiveTrigger(r));
    const hasCritical = critical.length > 0;
    const hasWatch = watchTriggered.length > 0;
    const shown = hasCritical ? critical : hasWatch ? watchTriggered : [];

    const bannerColor = hasCritical ? '#dc2626' : hasWatch ? '#eab308' : '#16a34a';
    const bannerLevel = hasCritical
        ? (critical.some(r => r.alertLevel === 'EXTREME') ? 'EXTREME' : 'DANGER')
        : hasWatch ? 'WATCH' : 'NORMAL';
    const headline = hasCritical
        ? `${describeCounts(critical)} at DANGER/EXTREME risk`
        : hasWatch
            ? `${describeCounts(watchTriggered)} under WATCH`
            : 'All monitored sites NORMAL';

    return (
        <div style={{ ...glassCard, padding: '0.85rem 1.25rem', borderLeft: `4px solid ${bannerColor}` }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                <AlertShapeIcon level={bannerLevel} size={22} />
                <h2 style={{ margin: 0, fontSize: '1.05rem' }}>{headline}</h2>
                <span style={{ ...mutedText, fontSize: '0.75rem' }}>· {risks.length} sites monitored</span>
            </div>

            {shown.length > 0 &&
                groupByBasin(shown).map(([basin, points]) => (
                    <BasinGroup key={basin} basin={basin} points={points} townsByBasin={townsByBasin} onSelect={onSelectRisk} />
                ))}

            {watchBaseline.length > 0 && (() => {
                const baselineLakes = watchBaseline.filter(r => r.sourceType !== 'GLACIER');
                const baselineGlaciers = watchBaseline.filter(r => r.sourceType === 'GLACIER');
                return (
                    <div style={{
                        ...mutedText, fontSize: '0.72rem', marginTop: shown.length > 0 ? '0.4rem' : '0.5rem',
                        display: 'flex', gap: '0.4rem',
                    }}>
                        <Info size={13} strokeWidth={2} style={{ flexShrink: 0, marginTop: '0.15rem' }} />
                        <span style={{ display: 'flex', flexDirection: 'column', gap: '0.15rem' }}>
                            {baselineLakes.length > 0 && (
                                <span>
                                    {baselineLakes.length} Lake{baselineLakes.length > 1 ? 's' : ''} (
                                    <ClickableNameList risks={baselineLakes} onSelect={onSelectRisk} />)
                                </span>
                            )}
                            {baselineGlaciers.length > 0 && (
                                <span>
                                    {baselineGlaciers.length} Glacier{baselineGlaciers.length > 1 ? 's' : ''} (
                                    <ClickableNameList risks={baselineGlaciers} onSelect={onSelectRisk} />)
                                </span>
                            )}
                            <span>
                                sitting in a seasonal high-risk window right now (real ICIMOD classification + monsoon
                                timing) but with no active trigger detected, so not counted above.
                            </span>
                        </span>
                    </div>
                );
            })()}
        </div>
    );
}
