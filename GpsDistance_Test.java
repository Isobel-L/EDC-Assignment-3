import org.junit.Test;
import static org.junit.Assert.*;

public class GpsDistance_Test {

    // Test 1: Identical GPS positions should have zero distance
    @Test
    public void testNoMovement() {
        GpsEvent start = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent end = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);

        assertEquals(0.0, GpsDistance.calculateDistance(start, end), 0.0001);
    }

    // Test 2: Convert altitude from feet to metres
    @Test
    public void testFeetToMetres() {
        assertEquals(0.0, GpsDistance.feetToMetres(0.0), 0.0001);
        assertEquals(0.3048, GpsDistance.feetToMetres(1.0), 0.0001);
        assertEquals(30.48, GpsDistance.feetToMetres(100.0), 0.0001);
    }

    // Test 3: Movement only in altitude
    @Test
    public void testAltitudeOnlyMovement() {
        GpsEvent start = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent end = new GpsEvent("Tracker0", 40.0, 116.0, 200.0);

        assertEquals(30.48, GpsDistance.calculateDistance(start, end), 0.0001);
    }

    // Test 4: Movement only in latitude
    @Test
    public void testLatitudeMovement() {
        GpsEvent start = new GpsEvent("Tracker0", 0.0, 0.0, 0.0);
        GpsEvent end = new GpsEvent("Tracker0", 1.0, 0.0, 0.0);

        // One degree of latitude is approximately 111.2 km
        assertEquals(111194.93,
                GpsDistance.calculateDistance(start, end), 1.0);
    }

    // Test 5: Movement only in longitude at the equator
    @Test
    public void testLongitudeMovement() {
        GpsEvent start = new GpsEvent("Tracker0", 0.0, 0.0, 0.0);
        GpsEvent end = new GpsEvent("Tracker0", 0.0, 1.0, 0.0);

        assertEquals(111194.93,
                GpsDistance.calculateDistance(start, end), 1.0);
    }

    // Test 6: Distance should be the same in either direction
    @Test
    public void testDistanceSymmetry() {
        GpsEvent start = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent end = new GpsEvent("Tracker0", 40.001, 116.001, 120.0);

        double forward = GpsDistance.calculateDistance(start, end);
        double backward = GpsDistance.calculateDistance(end, start);

        assertEquals(forward, backward, 0.0001);
    }

    // Test 7: Combined horizontal and vertical movement
    @Test
    public void testCombinedMovement() {
        GpsEvent start = new GpsEvent("Tracker0", 0.0, 0.0, 0.0);
        GpsEvent end = new GpsEvent("Tracker0", 0.001, 0.0, 100.0);

        double horizontal = Math.toRadians(0.001) * 6371000.0;
        double vertical = 30.48;
        double expected = Math.hypot(horizontal, vertical);

        assertEquals(expected,
                GpsDistance.calculateDistance(start, end), 0.0001);
    }

    // Test 8: Descending altitude should produce positive distance
    @Test
    public void testDescendingAltitude() {
        GpsEvent start = new GpsEvent("Tracker0", 40.0, 116.0, 200.0);
        GpsEvent end = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);

        assertEquals(30.48, GpsDistance.calculateDistance(start, end), 0.0001);
    }
}