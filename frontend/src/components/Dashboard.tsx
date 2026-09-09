import { useRef, useState } from 'react';
import { Mountain, Map as MapIcon, Info } from 'lucide-react';
import { Map, MapFocusTarget } from './Map';
import { AlertStatus } from './AlertStatus';
import { ClockWeatherCard } from './ClockWeatherCard';
import { SeismicActivityCard } from './SeismicActivityCard';
import { RiskGaugeCard } from './RiskGaugeCard';
import { RainfallTrendCard } from './RainfallTrendCard';
import { GlacierLakesCard } from './GlacierLakesCard';
import { DataSourcesCard } from './DataSourcesCard';
import { useGlofRiskMap } from '../api/useGlofRiskMap';
import { BACKGROUND_IMAGE_URL, glassCardPad, mutedText } from '../utils/theme';
import { findHighestRisk } from '../utils/risk';
import { useIsMobile } from '../utils/useIsMobile';

export function Dashboard() {
    const { risks } = useGlofRiskMap();
    const worst = findHighestRisk(risks);
    const isMobile = useIsMobile();
    const [focusTarget, setFocusTarget] = useState<MapFocusTarget | null>(null);
    const mapCardRef = useRef<HTMLDivElement>(null);
    const focusClickCount = useRef(0);

    // Every click gets a fresh token (even re-clicking the same name) so the
    // map's effect always re-fires, and scrolls the map into view - same
    // outcome as if you'd scrolled down and clicked the point yourself.
    function focusOnMap(riskId: number) {
        focusClickCount.current += 1;
        setFocusTarget({ riskId, token: focusClickCount.current });
        mapCardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    // Same behavior as focusOnMap, but for an earthquake/landslide marker -
    // those aren't in glofRisks, so the coordinates travel with the click
    // itself instead of being looked up.
    function focusOnSeismicEvent(event: { id: number; latitude: number; longitude: number }) {
        focusClickCount.current += 1;
        setFocusTarget({
            token: focusClickCount.current,
            seismic: { id: event.id, latitude: event.latitude, longitude: event.longitude },
        });
        mapCardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    return (
        <div
            style={{
                minHeight: '100vh',
                backgroundImage: `linear-gradient(180deg, rgba(10,14,20,0.55), rgba(10,14,20,0.85)), url(${BACKGROUND_IMAGE_URL})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                backgroundAttachment: 'fixed',
                color: '#f4f6f8',
                fontFamily: 'system-ui, -apple-system, sans-serif',
            }}
        >
            <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '2rem 1.5rem 3rem' }}>
                <header style={{ display: 'flex', justifyContent: 'flex-start', alignItems: 'center', marginBottom: '1.25rem', gap: '1rem', flexWrap: 'wrap' }}>
                    <div>
                        <h1 style={{ margin: 0, fontSize: '1.6rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <Mountain size={26} strokeWidth={2} /> Nepal Hazard Watch
                        </h1>
                        <div style={{ ...mutedText, fontSize: '0.75rem', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            Safer people · Stronger Nepal
                        </div>
                    </div>

                    <div
                        style={{
                            display: 'flex', alignItems: 'flex-start', gap: '0.5rem',
                            background: 'rgba(148, 163, 184, 0.12)',
                            border: '1px solid rgba(148, 163, 184, 0.3)',
                            borderRadius: '10px',
                            padding: '0.55rem 0.85rem',
                            maxWidth: '360px',
                            fontSize: '0.75rem',
                            lineHeight: 1.45,
                            color: 'rgba(244, 246, 248, 0.82)',
                        }}
                    >
                        <Info size={15} strokeWidth={2} style={{ flexShrink: 0, marginTop: '0.1rem', color: '#94a3b8' }} />
                        <span>
                            Independent educational project, not an official warning system.
                            For real emergencies, follow guidance from{' '}
                            <a
                                href="https://ndrrma.gov.np/en"
                                target="_blank"
                                rel="noopener noreferrer"
                                style={{ color: '#cbd5e1', textDecoration: 'underline' }}
                            >
                                Nepal's official disaster authorities
                            </a>.
                        </span>
                    </div>
                </header>

                {/* Grid keeps AlertStatus and the map in the same column width.
                    Below the mobile breakpoint it collapses to a single
                    column so the sidebar's 260px minimum can't squeeze the
                    map down to a sliver. */}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: isMobile ? 'minmax(0, 1fr)' : 'minmax(0, 3fr) minmax(260px, 1fr)',
                    gap: '1.5rem', marginBottom: '1.5rem',
                }}>
                    <div style={{ gridColumn: '1 / 2', gridRow: '1' }}>
                        <AlertStatus onSelectRisk={(risk) => focusOnMap(risk.id)} />
                    </div>

                    <div ref={mapCardRef} style={{ ...glassCardPad, gridColumn: '1 / 2', gridRow: '2' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                            <div style={{ fontWeight: 700, fontSize: '1.1rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                                <MapIcon size={19} strokeWidth={2} /> Live Hazard Map
                            </div>
                            <RiskGaugeCard risks={risks} />
                        </div>
                        <div style={{ marginTop: '0.75rem' }}>
                            <Map glofRisks={risks} focusTarget={focusTarget} />
                        </div>
                    </div>

                    {/* Sidebar widgets all follow the same highest-risk point. */}
                    <div style={{
                        gridColumn: isMobile ? '1 / 2' : '2 / 3',
                        gridRow: isMobile ? '3' : '2',
                        display: 'flex', flexDirection: 'column', gap: '1rem',
                    }}>
                        <ClockWeatherCard worst={worst} />
                        <RainfallTrendCard worst={worst} />
                        <SeismicActivityCard onSelectEvent={focusOnSeismicEvent} />
                    </div>
                </div>

                <GlacierLakesCard risks={risks} />

                <DataSourcesCard />
            </div>
        </div>
    );
}
