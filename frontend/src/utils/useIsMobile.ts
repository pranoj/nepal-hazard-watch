import { useEffect, useState } from 'react';

// Dashboard uses inline styles (no CSS files), so a CSS media query can't override it - tracks the breakpoint in JS instead
export function useIsMobile(breakpointPx = 860) {
    const query = `(max-width: ${breakpointPx}px)`;
    const [isMobile, setIsMobile] = useState(() => window.matchMedia(query).matches);

    useEffect(() => {
        const mql = window.matchMedia(query);
        const listener = () => setIsMobile(mql.matches);
        mql.addEventListener('change', listener);
        return () => mql.removeEventListener('change', listener);
    }, [query]);

    return isMobile;
}
