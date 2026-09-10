export function jitter(ms: number, spread = 5000) {
    return ms + Math.random() * spread;
}
