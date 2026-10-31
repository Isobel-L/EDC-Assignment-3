import nz.sodium.StreamSink;
import nz.sodium.Cell;
import org.junit.Test;

import static org.junit.Assert.*;

public class GpsHistoryFRP_Test {

    @Test
    public void testReactiveHistoryAccumulation() {

        // Simulate an incoming GPS event stream
        StreamSink<GpsHistory.TimedEvent> gpsInput =
            new StreamSink<>();

        // Sodium maintains the GPS history using accum()
        Cell<GpsHistory> historyCell = gpsInput.accum(
            new GpsHistory(),
            (event, history) -> history.add(event)
        );

        GpsEvent first = new GpsEvent(
            "Tracker0", 40.0, 116.0, 10.0
        );

        GpsEvent second = new GpsEvent(
            "Tracker0", 40.001, 116.0, 20.0
        );

        // Send two GPS events through Sodium
        gpsInput.send(
            new GpsHistory.TimedEvent(first, 0)
        );

        gpsInput.send(
            new GpsHistory.TimedEvent(second, 60000)
        );

        // Read the history maintained by Sodium
        GpsHistory history = historyCell.sample();

        assertEquals(2, history.size());

        GpsRestrictions unrestricted =
            new GpsRestrictions(90.0, 180.0);

        double expected =
            GpsDistance.calculateDistanceMetres(first, second);

        assertEquals(
            expected,
            history.calculateDistance(60000, unrestricted),
            0.0001
        );
    }
}