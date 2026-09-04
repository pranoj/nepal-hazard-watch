package watch.nepalhazard.dto;

public class GLOFRiskRequest {
    private Long earthquakeId;
    private double glacierLat;
    private double glacierLon;
    private String location;

    // Constructors
    public GLOFRiskRequest() {
    }

    public GLOFRiskRequest(Long earthquakeId, double glacierLat, double glacierLon, String location) {
        this.earthquakeId = earthquakeId;
        this.glacierLat = glacierLat;
        this.glacierLon = glacierLon;
        this.location = location;
    }

    // Getters and Setters
    public Long getEarthquakeId() {
        return earthquakeId;
    }

    public void setEarthquakeId(Long earthquakeId) {
        this.earthquakeId = earthquakeId;
    }

    public double getGlacierLat() {
        return glacierLat;
    }

    public void setGlacierLat(double glacierLat) {
        this.glacierLat = glacierLat;
    }

    public double getGlacierLon() {
        return glacierLon;
    }

    public void setGlacierLon(double glacierLon) {
        this.glacierLon = glacierLon;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}