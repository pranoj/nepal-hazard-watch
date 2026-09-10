import { useMemo } from 'react';
import { Mountain } from 'lucide-react';
import { GlofRiskAssessment } from '../api/useGlofRiskMap';
import { glassCardPad, mutedText, ALERT_LEVEL_LABEL } from '../utils/theme';

interface FeaturedLake {
    name: string;
    /** Real lake_name in our own glacial_lakes table, for matching to live risk data - undefined if not in our imported subset. */
    matchName?: string;
    caption: string;
    photoUrl: string;
    wikipediaUrl: string;
}

// small verified pool (real Wikimedia photo + Wikipedia coverage) instead of padding with unverifiable/dead links
const FEATURED_LAKES: FeaturedLake[] = [
    {
        name: 'Tsho Rolpa',
        caption: 'Nepal\'s largest, most closely monitored glacial lake, fitted with an artificial drainage channel since 2000',
        photoUrl: 'https://upload.wikimedia.org/wikipedia/commons/b/b3/Tsho_rolpa_lake.jpg',
        wikipediaUrl: 'https://en.wikipedia.org/wiki/Tsho_Rolpa',
    },
    {
        name: 'Imja Tsho',
        matchName: 'Chokarma Tsho',
        caption: 'Everest region, one of the Himalaya\'s most-studied glacial lakes',
        photoUrl: 'https://upload.wikimedia.org/wikipedia/commons/8/87/Imja_Tsho%2C_Nepal.jpg',
        wikipediaUrl: 'https://en.wikipedia.org/wiki/Imja_Tsho',
    },
    {
        name: 'Dig Tsho',
        matchName: 'Dig Tsho',
        caption: 'Real 1985 GLOF: destroyed a hydropower plant and 14 bridges, 5 deaths',
        photoUrl:
            'https://upload.wikimedia.org/wikipedia/commons/b/b8/ISS066-E-86263_-_View_of_Nepal_-_Drolambao_Glacier_-_Drangnag_Ri_-_Rolwaling_Glacier_-_Chobuje_%28Tsoboje%29_-_Trakarding_Glacier_-_Tsho_Rolpa_Lake_-_Dragkar_Go_%28Takargo%29_-_Tengi_Ragi_Tau_-_Dig_Tsho_Lake_-_Chhule_Glacier_%28cropped%29.jpg',
        // no dedicated Wikipedia article for Dig Tsho - links to where it's substantively covered instead
        wikipediaUrl: 'https://en.wikipedia.org/wiki/Glacial_lake_outburst_flood',
    },
    {
        name: 'Birendra Lake',
        matchName: 'Birendra Taal',
        caption: 'Popular high-altitude lake on the Manaslu Circuit trek, below Mt. Manaslu',
        photoUrl: 'https://upload.wikimedia.org/wikipedia/commons/5/58/Birendra_lake_and_Mt._Manaslu.jpg',
        wikipediaUrl: 'https://en.wikipedia.org/wiki/Birendra_Lake',
    },
];

function shuffled<T>(items: T[]): T[] {
    const copy = [...items];
    for (let i = copy.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [copy[i], copy[j]] = [copy[j], copy[i]];
    }
    return copy;
}

interface GlacierLakesCardProps {
    risks: GlofRiskAssessment[];
}

export function GlacierLakesCard({ risks }: GlacierLakesCardProps) {
    // shuffled once per page load - no auto-rotating timer to distract from while reading
    const order = useMemo(() => shuffled(FEATURED_LAKES), []);

    return (
        <div style={{ ...glassCardPad, marginTop: '1.5rem' }}>
            <div style={{ fontWeight: 700, fontSize: '1.05rem', display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.9rem' }}>
                <Mountain size={19} strokeWidth={2} /> Notable Glacial Lakes
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1.25rem' }}>
                {order.map((lake) => {
                    const liveMatch = lake.matchName
                        ? risks.find((r) => r.lakeName.toLowerCase() === lake.matchName!.toLowerCase())
                        : undefined;
                    return (
                        <a
                            key={lake.name}
                            href={lake.wikipediaUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{ textDecoration: 'none', color: 'inherit' }}
                        >
                            <img
                                src={lake.photoUrl}
                                alt={lake.name}
                                style={{ width: '100%', height: '130px', objectFit: 'cover', borderRadius: '10px' }}
                            />
                            <div style={{ fontWeight: 600, marginTop: '0.5rem', fontSize: '0.95rem' }}>{lake.name}</div>
                            <div style={{ ...mutedText, fontSize: '0.75rem' }}>{lake.caption}</div>
                            {liveMatch ? (
                                <div style={{ fontSize: '0.75rem', color: '#eab308', marginTop: '0.2rem' }}>
                                    Live: {liveMatch.riskScore.toFixed(0)}/100 · {ALERT_LEVEL_LABEL[liveMatch.alertLevel]}
                                </div>
                            ) : (
                                <div style={{ ...mutedText, fontSize: '0.7rem', marginTop: '0.2rem' }}>
                                    Not in our monitored subset
                                </div>
                            )}
                        </a>
                    );
                })}
            </div>
            <div style={{ ...mutedText, fontSize: '0.65rem', marginTop: '0.9rem' }}>
                Photos: Wikimedia Commons (CC-licensed)
            </div>
        </div>
    );
}
