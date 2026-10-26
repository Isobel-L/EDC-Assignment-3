
public class GpsDistance {

    // Approximate radius of the Earth in metres
    private static final double EARTH_RADIUS_METRES = 6371000.0;

    // Convert altitude from feet to metres
    public static double feetToMetres(double feet) {
        return feet * 0.3048;
    }

    // Calculate the distance between two GPS positions
    public static double calculateDistance(
            GpsEvent start, GpsEvent end) {

        // Convert latitude and longitude differences to radians
        double latitudeDifference = Math.toRadians(
            end.latitude - start.latitude
        );

        double longitudeDifference = Math.toRadians(
            end.longitude - start.longitude
        );

        // Use average latitude for longitude distance conversion
        double averageLatitude = Math.toRadians(
            (start.latitude + end.latitude) / 2.0
        );

        // Calculate north-south movement in metres
        double northSouthDistance =
            latitudeDifference * EARTH_RADIUS_METRES;

        // Calculate east-west movement in metres
        double eastWestDistance =
            longitudeDifference * EARTH_RADIUS_METRES
            * Math.cos(averageLatitude);

        // Calculate total horizontal distance
        double horizontalDistance = Math.hypot(
            northSouthDistance,
            eastWestDistance
        );

        // Convert altitude difference from feet to metres
        double altitudeDifference = feetToMetres(
            end.altitude - start.altitude
        );

        // Calculate total 3D distance including altitude
        return Math.hypot(
            horizontalDistance,
            altitudeDifference
        );
    }
}
