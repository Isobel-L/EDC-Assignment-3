import org.junit.Test;
import static org.junit.Assert.*;

public class GpsRestrictions_Test {

    // Test 1: GPS event inside both restrictions
    @Test
    public void testEventInsideRange() {
        GpsRestrictions restrictions = new GpsRestrictions(40.0, 116.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 39.5, 115.0, 100.0
        );

        assertTrue(restrictions.contains(event));
    }

    // Test 2: GPS event outside latitude restriction
    @Test
    public void testLatitudeOutsideRange() {
        GpsRestrictions restrictions = new GpsRestrictions(40.0, 116.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 41.0, 115.0, 100.0
        );

        assertFalse(restrictions.contains(event));
    }

    // Test 3: GPS event outside longitude restriction
    @Test
    public void testLongitudeOutsideRange() {
        GpsRestrictions restrictions = new GpsRestrictions(40.0, 116.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 39.5, 117.0, 100.0
        );

        assertFalse(restrictions.contains(event));
    }

    // Test 4: GPS event outside both restrictions
    @Test
    public void testBothCoordinatesOutsideRange() {
        GpsRestrictions restrictions = new GpsRestrictions(40.0, 116.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 45.0, 120.0, 100.0
        );

        assertFalse(restrictions.contains(event));
    }

    // Test 5: Event exactly on positive boundaries
    @Test
    public void testPositiveBoundaries() {
        GpsRestrictions restrictions = new GpsRestrictions(40.0, 116.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 40.0, 116.0, 100.0
        );

        assertTrue(restrictions.contains(event));
    }

    // Test 6: Event exactly on negative boundaries
    @Test
    public void testNegativeBoundaries() {
        GpsRestrictions restrictions = new GpsRestrictions(40.0, 116.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", -40.0, -116.0, 100.0
        );

        assertTrue(restrictions.contains(event));
    }

    // Test 7: Restriction of zero accepts origin
    @Test
    public void testZeroRestrictionAcceptsOrigin() {
        GpsRestrictions restrictions = new GpsRestrictions(0.0, 0.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 0.0, 0.0, 100.0
        );

        assertTrue(restrictions.contains(event));
    }

    // Test 8: Restriction of zero rejects other coordinates
    @Test
    public void testZeroRestrictionRejectsMovement() {
        GpsRestrictions restrictions = new GpsRestrictions(0.0, 0.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 0.001, 0.0, 100.0
        );

        assertFalse(restrictions.contains(event));
    }

    // Test 9: Altitude should not affect coordinate filtering
    @Test
    public void testAltitudeDoesNotAffectFiltering() {
        GpsRestrictions restrictions = new GpsRestrictions(40.0, 116.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", 39.5, 115.0, 10000.0
        );

        assertTrue(restrictions.contains(event));
    }

    // Test 10: Full restrictions accept valid negative coordinates
    @Test
    public void testFullCoordinateRange() {
        GpsRestrictions restrictions = new GpsRestrictions(90.0, 180.0);

        GpsEvent event = new GpsEvent(
            "Tracker0", -85.0, -175.0, 100.0
        );

        assertTrue(restrictions.contains(event));
    }
}