import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import nz.sodium.Stream;

public class GpsGUI extends JFrame {

    public GpsGUI() {
        // Set up the main application window
        setTitle("GPS Tracking System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Temporary heading to confirm the GUI works
        JLabel heading = new JLabel(
            "GPS Tracking System",
            JLabel.CENTER
        );

        add(heading, BorderLayout.NORTH);

        // Create a panel to hold all 10 GPS trackers
        JPanel trackerPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        // Create a separate display for each tracker
        for (int i = 0; i < 10; i++) {

            // Each tracker gets its own panel
            JPanel individualTracker = new JPanel(new GridLayout(4, 1));

            // Display the tracker name and coordinate fields
            JLabel nameLabel = new JLabel("Tracker" + i);
            JLabel latitudeLabel = new JLabel("Latitude: --");
            JLabel longitudeLabel = new JLabel("Longitude: --");
            JLabel eventLabel = new JLabel("Latest Event: --");

            individualTracker.add(nameLabel);
            individualTracker.add(latitudeLabel);
            individualTracker.add(longitudeLabel);
            individualTracker.add(eventLabel);

            // Add a visible border around each tracker
            individualTracker.setBorder(
                javax.swing.BorderFactory.createEtchedBorder()
            );

            trackerPanel.add(individualTracker);
        }

        // Add all tracker displays to the main window
        add(trackerPanel, BorderLayout.CENTER);

        // Connect to the GPS service
        GpsService service = new GpsService();
        Stream<GpsEvent>[] streams = service.getEventStreams();

        // Connect each GPS stream to its corresponding tracker panel
        for (int i = 0; i < 10; i++) {

            // Remove altitude from the event using Sodium FRP
            Stream<String> simplifiedStream = streams[i].map(event ->
                event.name + "," + event.latitude + "," + event.longitude
            );

            // Find the GUI panel corresponding to this tracker
            JPanel individualTracker = (JPanel) trackerPanel.getComponent(i);

            JLabel latitudeLabel = (JLabel) individualTracker.getComponent(1);
            JLabel longitudeLabel = (JLabel) individualTracker.getComponent(2);
            JLabel eventLabel = (JLabel) individualTracker.getComponent(3);

            // Each tracker needs its own independent clearing timer
            Timer clearTimer = new Timer(3000, e ->
                eventLabel.setText("Latest Event: --")
            );
            clearTimer.setRepeats(false);

            // Listen for incoming GPS events
            simplifiedStream.listen(data -> {
                SwingUtilities.invokeLater(() -> {

                    // Extract the coordinates from the simplified event
                    String[] fields = data.split(",");

                    latitudeLabel.setText("Latitude: " + fields[1]);
                    longitudeLabel.setText("Longitude: " + fields[2]);
                    eventLabel.setText("Latest Event: " + data);

                    // Reset this tracker's timer when a new event arrives
                    clearTimer.restart();
                });
            });
        }
    }

    public static void main(String[] args) {
        // Start the GUI on Swing's Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            GpsGUI gui = new GpsGUI();
            gui.setVisible(true);
        });
    }
}