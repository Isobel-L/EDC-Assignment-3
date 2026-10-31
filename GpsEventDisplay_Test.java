import org.junit.Test;
import static org.junit.Assert.*;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class GpsEventDisplay_Test {

    @Test
    public void testEventClearsAfterThreeSeconds() throws Exception {

        JLabel label = new JLabel("Latest Event: --");
        GpsEventDisplay display = new GpsEventDisplay(label);

        try {
            // Display a new GPS event
            SwingUtilities.invokeAndWait(() ->
                display.showEvent("Tracker0,40.0,116.0")
            );

            assertEquals(
                "Latest Event: Tracker0,40.0,116.0",
                label.getText()
            );

            // Wait slightly longer than three seconds
            Thread.sleep(3300);

            // Ensure pending Swing events have been processed
            SwingUtilities.invokeAndWait(() -> {});

            // The recent event should now be cleared
            assertEquals(
                "Latest Event: --",
                label.getText()
            );

        } finally {
            SwingUtilities.invokeAndWait(display::stop);
        }
    }

    @Test
    public void testNewEventRestartsTimer() throws Exception {

        JLabel label = new JLabel("Latest Event: --");
        GpsEventDisplay display = new GpsEventDisplay(label);

        try {
            SwingUtilities.invokeAndWait(() ->
                display.showEvent("First Event")
            );

            // Wait before sending another event
            Thread.sleep(1500);

            SwingUtilities.invokeAndWait(() ->
                display.showEvent("Second Event")
            );

            // More than three seconds have passed since
            // the first event, but not since the second
            Thread.sleep(1800);

            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(
                "Latest Event: Second Event",
                label.getText()
            );

            // Wait until three seconds after the second event
            Thread.sleep(1500);

            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(
                "Latest Event: --",
                label.getText()
            );

        } finally {
            SwingUtilities.invokeAndWait(display::stop);
        }
    }
}