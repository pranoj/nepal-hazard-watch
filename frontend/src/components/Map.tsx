import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Region } from '../api/useRegions';
import { HazardEvent } from '../api/useHazardEvents';

// Fix marker icons
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

interface MapProps {
    regions: Region[];
    hazardEvents: HazardEvent[];
}

export function Map({ regions, hazardEvents }: MapProps) {
    const center: [number, number] = [28.5, 85.5];

    return (
        <div style={{ marginTop: '2rem' }}>
            <h2>🗺️ Regional Risk Map</h2>
            <MapContainer center={center} zoom={7} style={{ height: '500px', borderRadius: '8px' }}>
                <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

                {regions.map((region) => (
                    <Marker key={region.id} position={[region.latitude, region.longitude]}>
                        <Popup>{region.name} - {region.riskLevel} Risk</Popup>
                    </Marker>
                ))}

                {hazardEvents.map((event) => (
                    <Marker key={event.id} position={[event.latitude, event.longitude]}>
                        <Popup>⚠️ {event.eventType} - {event.status}</Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}