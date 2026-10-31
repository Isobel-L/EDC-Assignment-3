import javax.swing.JLabel;
import javax.swing.Timer;

public class GpsEventDisplay {

    private static final int CLEAR_DELAY_MS = 3000;

    private final JLabel eventLabel;
    private final Timer clearTimer;

    public GpsEventDisplay(JLabel eventLabel) {

        this.eventLabel = eventLabel;

        this.clearTimer = new Timer(
            CLEAR_DELAY_MS,
            event -> clear()
        );

        this.clearTimer.setRepeats(false);
    }

    // Display a new event and restart the three-second countdown
    public void showEvent(String eventText) {

        eventLabel.setText("Latest Event: " + eventText);

        clearTimer.restart();
    }

    // Clear the event after three seconds of inactivity
    public void clear() {

        clearTimer.stop();

        eventLabel.setText("Latest Event: --");
    }

    // Stop the timer when the display is no longer needed
    public void stop() {

        clearTimer.stop();
    }
}