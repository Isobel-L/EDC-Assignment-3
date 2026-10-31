import nz.sodium.Stream;
import nz.sodium.StreamSink;
import nz.sodium.Listener;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class GpsAltitudeFRP_Test {

    @Test
    public void testAltitudeConversionUsingMap() {

        // Create a controllable GPS event stream
        StreamSink<GpsEvent> gpsInput = new StreamSink<>();

        // Convert altitude from feet to metres using Sodium map()
        Stream<GpsEvent> convertedStream = gpsInput.map(event -> new GpsEvent(
                event.name,
                event.latitude,
                event.longitude,
                GpsDistance.feetToMetres(event.altitude)));

        // Collect converted GPS events
        List<GpsEvent> results = new ArrayList<>();

        Listener listener = convertedStream.listen(results::add);

        try {
            // Send a GPS event with altitude of 100 feet
            gpsInput.send(
                    new GpsEvent("Tracker0", 40.0, 116.0, 100.0));

            assertEquals(1, results.size());

            GpsEvent converted = results.get(0);

            // 100 feet = 30.48 metres
            assertEquals(30.48, converted.altitude, 0.0001);

            // Other GPS properties must remain unchanged
            assertEquals("Tracker0", converted.name);
            assertEquals(40.0, converted.latitude, 0.0001);
            assertEquals(116.0, converted.longitude, 0.0001);

        } finally {
            listener.unlisten();
        }
    }

    @Test
    public void testDistanceWithAltitudeAlreadyInMetres() {

        // The true parameter indicates altitude is already in metres
        GpsDistanceWindow window = new GpsDistanceWindow(true);

        GpsEvent first = new GpsEvent(
                "Tracker0", 40.0, 116.0, 0.0);

        GpsEvent second = new GpsEvent(
                "Tracker0", 40.0, 116.0, 30.48);

        window.addEvent(first, 0);
        window.addEvent(second, 1000);

        // 30.48 metres of vertical movement
        assertEquals(
                30.48,
                window.getDistance(1000),
                0.0001);

        // Round upwards to 31 metres
        assertEquals(
                31L,
                window.getRoundedDistance(1000));
    }
}