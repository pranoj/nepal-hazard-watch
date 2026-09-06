import { GlofRiskAssessment } from '../api/useGlofRiskMap';

export function findHighestRisk(risks: GlofRiskAssessment[]): GlofRiskAssessment | null {
    return risks.length > 0
        ? risks.reduce((max, r) => (r.riskScore > max.riskScore ? r : max), risks[0])
        : null;
}

// A WATCH score can come from a real trigger (rain, quake, landslide) or
// just a lake's static classification plus monsoon season - lake-type +
// season maxes out at 30 points, below the 50-point DANGER floor, so only
// WATCH can happen on static factors alone. The earthquake threshold (not
// "> 0") filters out the near-zero noise a distant, weak quake leaves behind.
const EARTHQUAKE_ACTIVE_THRESHOLD = 0.15;

export function hasActiveTrigger(risk: GlofRiskAssessment): boolean {
    return risk.landslideDetected
        || risk.landslidePreCondition
        || risk.rainfallCondition !== 'NORMAL'
        || risk.earthquakeComponent > EARTHQUAKE_ACTIVE_THRESHOLD;
}
