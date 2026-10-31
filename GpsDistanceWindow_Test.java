import org.junit.Test;
import static org.junit.Assert.*;

public class GpsDistanceWindow_Test {

    @Test
    public void testInitialDistance() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);

        window.addEvent(first, 0);

        assertEquals(0.0, window.getDistance(0), 0.0001);
    }

    @Test
    public void testMultipleMovements() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent second = new GpsEvent("Tracker0", 40.001, 116.0, 100.0);
        GpsEvent third = new GpsEvent("Tracker0", 40.002, 116.0, 100.0);

        window.addEvent(first, 0);
        window.addEvent(second, 60000);
        window.addEvent(third, 120000);

        double expected = GpsDistance.calculateDistance(first, second)
                + GpsDistance.calculateDistance(second, third);

        assertEquals(expected, window.getDistance(120000), 0.0001);
    }

    @Test
    public void testFiveMinuteExpiry() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent second = new GpsEvent("Tracker0", 40.001, 116.0, 100.0);

        window.addEvent(first, 0);
        window.addEvent(second, 60000);

        assertTrue(window.getDistance(300000) > 0);

        // The segment expires after five minutes from its timestamp
        assertEquals(0.0, window.getDistance(360001), 0.0001);
    }

    @Test
    public void testRoundedDistance() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 40.0, 116.0, 0.0);
        GpsEvent second = new GpsEvent("Tracker0", 40.0, 116.0, 1.0);

        window.addEvent(first, 0);
        window.addEvent(second, 1000);

        // One foot = 0.3048 metres, rounded up to 1 metre
        assertEquals(1L, window.getRoundedDistance(1000));
    }

    @Test
    public void testReset() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent second = new GpsEvent("Tracker0", 40.001, 116.0, 100.0);

        window.addEvent(first, 0);
        window.addEvent(second, 60000);

        window.reset();

        assertEquals(0.0, window.getDistance(60000), 0.0001);
    }

    @Test
    public void testExactFiveMinuteBoundary() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent second = new GpsEvent("Tracker0", 40.001, 116.0, 100.0);

        window.addEvent(first, 0);
        window.addEvent(second, 60000);

        double expected = GpsDistance.calculateDistance(first, second);

        // Both positions are initially inside the window
        assertEquals(expected, window.getDistance(300000), 0.0001);

        // At 300001 ms, the first position has expired
        assertEquals(0.0, window.getDistance(300001), 0.0001);

        // Only the second position remains at 360000 ms
        assertEquals(0.0, window.getDistance(360000), 0.0001);
    }

    @Test
    public void testLongGapBetweenEvents() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 40.0, 116.0, 100.0);
        GpsEvent second = new GpsEvent("Tracker0", 40.001, 116.0, 100.0);
        GpsEvent third = new GpsEvent("Tracker0", 40.010, 116.0, 100.0);

        // First two events are one minute apart
        window.addEvent(first, 0);
        window.addEvent(second, 60000);

        assertTrue(window.getDistance(60000) > 0);

        // Third event arrives seven minutes after the second
        window.addEvent(third, 480000);

        // Previous movement has expired, and no distance
        // should be calculated across the seven-minute gap
        assertEquals(0.0, window.getDistance(480000), 0.0001);
    }

    @Test
    public void testDistanceRecalculationAfterRestrictionChange() {
        GpsDistanceWindow window = new GpsDistanceWindow();

        GpsEvent first = new GpsEvent("Tracker0", 39.0, 115.0, 100.0);
        GpsEvent second = new GpsEvent("Tracker0", 39.001, 115.0, 100.0);
        GpsEvent third = new GpsEvent("Tracker0", 41.0, 117.0, 100.0);
        GpsEvent fourth = new GpsEvent("Tracker0", 39.002, 115.0, 100.0);

        window.addEvent(first, 0);
        window.addEvent(second, 60000);
        window.addEvent(third, 120000);
        window.addEvent(fourth, 180000);

        // Initially allow all GPS coordinates
        GpsRestrictions unrestricted = new GpsRestrictions(90.0, 180.0);

        double fullDistance = window.getDistance(180000, unrestricted);

        // Restrict coordinates to exclude the third position
        GpsRestrictions restricted = new GpsRestrictions(40.0, 116.0);

        double expected = GpsDistance.calculateDistance(first, second)
                + GpsDistance.calculateDistance(second, fourth);

        double filteredDistance = window.getDistance(180000, restricted);

        assertEquals(expected, filteredDistance, 0.0001);

        // Excluding a position changes the calculated route
        assertNotEquals(fullDistance, filteredDistance, 0.0001);

        // Restore unrestricted coordinates without losing GPS history
        assertEquals(
                fullDistance,
                window.getDistance(180000, unrestricted),
                0.0001);
    }

    @Test
    public void testDistanceWithAltitudeAlreadyInMetres() {

        // Altitude values have already been converted into metres
        GpsDistanceWindow window = new GpsDistanceWindow(true);

        GpsEvent first = new GpsEvent(
                "Tracker0", 40.0, 116.0, 0.0);

        GpsEvent second = new GpsEvent(
                "Tracker0", 40.0, 116.0, 30.48);

        window.addEvent(first, 0);
        window.addEvent(second, 1000);

        // Verify the altitude is not converted twice
        assertEquals(
                30.48,
                window.getDistance(1000),
                0.0001);

        // 30.48 metres should round up to 31 metres
        assertEquals(
                31L,
                window.getRoundedDistance(1000));
    }
}