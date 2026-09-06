import type { LucideIcon } from 'lucide-react';
import { Satellite, Activity, CloudRain, Mountain, Snowflake, FileText } from 'lucide-react';
import { glassCardPad, mutedText, BACKGROUND_IMAGE_ATTRIBUTION } from '../utils/theme';

interface DataSource {
    Icon: LucideIcon;
    name: string;
    org: string;
    usage: string;
    url: string;
}

// Real sources actually feeding this app - keep in sync if one stops being used.
const DATA_SOURCES: DataSource[] = [
    {
        Icon: Activity, name: 'USGS', org: 'U.S. Geological Survey',
        usage: 'Real-time earthquake & landslide detection',
        url: 'https://earthquake.usgs.gov/',
    },
    {
        Icon: CloudRain, name: 'OpenWeatherMap', org: 'OpenWeather Ltd.',
        usage: 'Live temperature, humidity & rainfall readings',
        url: 'https://openweathermap.org/',
    },
    {
        Icon: Mountain, name: 'ICIMOD', org: 'Intl. Centre for Integrated Mountain Development',
        usage: 'Glacial lake inventory & GLOF risk classification (HMA GLOF Database)',
        url: 'https://www.icimod.org/',
    },
    {
        Icon: Snowflake, name: 'RGI', org: 'Randolph Glacier Inventory / GLIMS Consortium',
        usage: 'Glacier boundaries, slope & area data',
        url: 'https://www.glims.org/',
    },
    {
        Icon: Satellite, name: 'Esri', org: 'Esri, Maxar, Earthstar Geographics',
        usage: 'Satellite map imagery & place labels',
        url: 'https://www.esri.com/',
    },
];

export function DataSourcesCard() {
    return (
        <div style={{ ...glassCardPad, marginTop: '1.5rem' }}>
            <div style={{ fontWeight: 700, fontSize: '1.05rem', marginBottom: '0.2rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Satellite size={19} strokeWidth={2} /> Data Sources & Credits
            </div>
            <div style={{ ...mutedText, fontSize: '0.75rem', marginBottom: '0.9rem' }}>
                Every figure on this page traces back to one of these real, independently-verifiable sources.
            </div>
            <a
                href="/methodology.html"
                target="_blank"
                rel="noopener noreferrer"
                style={{
                    display: 'inline-flex', alignItems: 'center', gap: '0.4rem', textDecoration: 'none', color: '#ffffff', fontSize: '0.85rem', fontWeight: 700,
                    padding: '0.6rem 1.1rem', borderRadius: '10px', background: '#2563eb',
                    boxShadow: '0 2px 8px rgba(37,99,235,0.4)', marginBottom: '1.1rem',
                }}
            >
                <FileText size={15} strokeWidth={2} /> Read the full methodology
            </a>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                {DATA_SOURCES.map((src) => (
                    <a
                        key={src.name}
                        href={src.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{
                            display: 'flex', gap: '0.6rem', textDecoration: 'none', color: 'inherit',
                            padding: '0.6rem 0.7rem', borderRadius: '10px', background: 'rgba(255,255,255,0.04)',
                            border: '1px solid rgba(255,255,255,0.08)',
                        }}
                    >
                        <src.Icon size={22} strokeWidth={1.75} style={{ flexShrink: 0, color: 'rgba(244,246,248,0.85)' }} />
                        <div style={{ minWidth: 0 }}>
                            <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{src.name}</div>
                            <div style={{ ...mutedText, fontSize: '0.68rem' }}>{src.org}</div>
                            <div style={{ fontSize: '0.72rem', marginTop: '0.2rem' }}>{src.usage}</div>
                        </div>
                    </a>
                ))}
            </div>
            <div style={{ ...mutedText, fontSize: '0.65rem', marginTop: '0.9rem' }}>
                Background photo: {BACKGROUND_IMAGE_ATTRIBUTION}
            </div>
        </div>
    );
}
