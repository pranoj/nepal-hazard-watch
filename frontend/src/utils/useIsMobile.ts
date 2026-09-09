import { useEffect, useState } from 'react';

// Dashboard's layout uses inline styles (no CSS files in this project), so a
// CSS media query can't override it - this tracks the breakpoint in JS
// instead so Dashboard can switch its grid to a single column on narrow
// screens.
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
