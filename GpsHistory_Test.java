import org.junit.Test;
import static org.junit.Assert.*;

public class GpsHistory_Test {

    @Test
    public void testHistoryAccumulatesDistance() {

        GpsHistory history = new GpsHistory();

        // GPS altitude values are already converted to metres
        GpsEvent first = new GpsEvent(
            "Tracker0", 40.0, 116.0, 10.0
        );

        GpsEvent second = new GpsEvent(
            "Tracker0", 40.001, 116.0, 20.0
        );

        // Add two GPS positions one minute apart
        history = history.add(
            new GpsHistory.TimedEvent(first, 0)
        );

        history = history.add(
            new GpsHistory.TimedEvent(second, 60000)
        );

        // Calculate the expected 3D distance
        double expected = GpsDistance.calculateDistanceMetres(
            first, second
        );

        GpsRestrictions unrestricted =
            new GpsRestrictions(90.0, 180.0);

        assertEquals(
            expected,
            history.calculateDistance(60000, unrestricted),
            0.0001
        );

        assertEquals(2, history.size());
    }
}