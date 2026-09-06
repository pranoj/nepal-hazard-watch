import { Mountain, Map as MapIcon } from 'lucide-react';
import { Map } from './Map';
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

export function Dashboard() {
    const { risks } = useGlofRiskMap();
    const worst = findHighestRisk(risks);

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
                <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                    <div>
                        <h1 style={{ margin: 0, fontSize: '1.6rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <Mountain size={26} strokeWidth={2} /> Nepal Hazard Watch
                        </h1>
                        <div style={{ ...mutedText, fontSize: '0.75rem', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            Safer people · Stronger Nepal
                        </div>
                    </div>
                </header>

                {/* Grid keeps AlertStatus and the map in the same column width. */}
                <div style={{
                    display: 'grid', gridTemplateColumns: 'minmax(0, 3fr) minmax(260px, 1fr)',
                    gap: '1.5rem', marginBottom: '1.5rem',
                }}>
                    <div style={{ gridColumn: '1 / 2', gridRow: '1' }}>
                        <AlertStatus />
                    </div>

                    <div style={{ ...glassCardPad, gridColumn: '1 / 2', gridRow: '2' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
                            <div style={{ fontWeight: 700, fontSize: '1.1rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                                <MapIcon size={19} strokeWidth={2} /> Live Hazard Map
                            </div>
                            <RiskGaugeCard risks={risks} />
                        </div>
                        <div style={{ marginTop: '0.75rem' }}>
                            <Map glofRisks={risks} />
                        </div>
                    </div>

                    {/* Sidebar widgets all follow the same highest-risk point. */}
                    <div style={{
                        gridColumn: '2 / 3', gridRow: '2',
                        display: 'flex', flexDirection: 'column', gap: '1rem',
                    }}>
                        <ClockWeatherCard worst={worst} />
                        <RainfallTrendCard worst={worst} />
                        <SeismicActivityCard />
                    </div>
                </div>

                <GlacierLakesCard risks={risks} />

                <DataSourcesCard />
            </div>
        </div>
    );
}
