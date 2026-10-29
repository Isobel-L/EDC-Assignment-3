public class GpsRestrictions {

    private final double latitude;
    private final double longitude;

    public GpsRestrictions(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Check whether a GPS event satisfies both restrictions
    public boolean contains(GpsEvent event) {
        return Math.abs(event.latitude) <= latitude
            && Math.abs(event.longitude) <= longitude;
    }
}