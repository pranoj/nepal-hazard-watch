import { GlofRiskAssessment } from '../api/useGlofRiskMap';

export function findHighestRisk(risks: GlofRiskAssessment[]): GlofRiskAssessment | null {
    return risks.length > 0
        ? risks.reduce((max, r) => (r.riskScore > max.riskScore ? r : max), risks[0])
        : null;
}

// lake-type + season maxes out at 30 pts (below the 50-pt DANGER floor), so only WATCH can happen on static factors alone
const EARTHQUAKE_ACTIVE_THRESHOLD = 0.15;

export function hasActiveTrigger(risk: GlofRiskAssessment): boolean {
    return risk.landslideDetected
        || risk.landslidePreCondition
        || risk.rainfallCondition !== 'NORMAL'
        || risk.earthquakeComponent > EARTHQUAKE_ACTIVE_THRESHOLD;
}
