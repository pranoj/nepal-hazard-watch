// Backend stores eventTime as UTC but serializes it without a "Z" suffix -
// without this, `new Date(...)` would parse it as the browser's own local
// time instead of UTC.
export function parseUtcEventTime(isoLocal: string): Date {
    return new Date(isoLocal.endsWith('Z') ? isoLocal : `${isoLocal}Z`);
}

export function formatNepalTime(isoLocal: string): string {
    const formatted = parseUtcEventTime(isoLocal).toLocaleString('en-US', {
        timeZone: 'Asia/Kathmandu',
        month: 'numeric', day: 'numeric', year: 'numeric',
        hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true,
    });
    return `${formatted} NPT (GMT+5:45)`;
}

export function minutesAgo(isoLocal: string): number {
    const diffMs = Date.now() - parseUtcEventTime(isoLocal).getTime();
    return Math.max(0, Math.floor(diffMs / 60000));
}

export function hoursAgo(isoLocal: string): number {
    return minutesAgo(isoLocal) / 60;
}

export function formatTimeAgo(minutes: number): string {
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

export function extractNearbyArea(description: string): string {
    const withoutRiskAssessment = description.split(' | ')[0];
    const withoutDepth = withoutRiskAssessment.replace(/\s*\(Depth:.*\)$/, '');
    const dashIndex = withoutDepth.indexOf(' - ');
    return dashIndex >= 0 ? withoutDepth.slice(dashIndex + 3) : withoutDepth;
}
