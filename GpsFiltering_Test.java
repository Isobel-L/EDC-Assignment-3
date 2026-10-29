import nz.sodium.CellSink;
import nz.sodium.Stream;
import nz.sodium.StreamSink;
import nz.sodium.Listener;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class GpsFiltering_Test {

    @Test
    public void testSodiumFiltering() {

        // Create a controllable GPS event stream
        StreamSink<GpsEvent> gpsInput = new StreamSink<>();

        // Set initial coordinate restrictions
        CellSink<GpsRestrictions> restrictions =
            new CellSink<>(new GpsRestrictions(40.0, 116.0));

        // Apply the same Sodium transformations used in GpsGUI
        Stream<GpsEvent> checkedStream = gpsInput.snapshot(
            restrictions,
            (event, currentRestrictions) ->
                currentRestrictions.contains(event) ? event : null
        );

        Stream<GpsEvent> filteredStream =
            checkedStream.filter(event -> event != null);

        // Collect accepted events for verification
        List<GpsEvent> acceptedEvents = new ArrayList<>();

        Listener listener = filteredStream.listen(
            event -> acceptedEvents.add(event)
        );

        try {
            // Event inside restrictions: accepted
            gpsInput.send(
                new GpsEvent("Tracker0", 39.5, 115.0, 100.0)
            );

            // Event outside latitude restriction: rejected
            gpsInput.send(
                new GpsEvent("Tracker0", 45.0, 115.0, 100.0)
            );

            // Event outside longitude restriction: rejected
            gpsInput.send(
                new GpsEvent("Tracker0", 39.5, 120.0, 100.0)
            );

            assertEquals(1, acceptedEvents.size());

            // Change restrictions reactively
            restrictions.send(new GpsRestrictions(90.0, 180.0));

            // Previously excluded coordinates should now be accepted
            gpsInput.send(
                new GpsEvent("Tracker0", 45.0, 120.0, 100.0)
            );

            assertEquals(2, acceptedEvents.size());

        } finally {
            // Release the Sodium listener
            listener.unlisten();
        }
    }
}