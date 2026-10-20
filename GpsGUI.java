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

        // Transform Tracker0's GPS events to remove altitude
        Stream<String> simplifiedStream = streams[0].map(event ->
            event.name + "," + event.latitude + "," + event.longitude
        );

        // Find Tracker0's panel and its labels
        JPanel tracker0Panel = (JPanel) trackerPanel.getComponent(0);

        JLabel latitude0 = (JLabel) tracker0Panel.getComponent(1);
        JLabel longitude0 = (JLabel) tracker0Panel.getComponent(2);
        JLabel event0 = (JLabel) tracker0Panel.getComponent(3);

        // Clear the latest-event field after 3 seconds
        Timer clearTimer = new Timer(3000, e ->
            event0.setText("Latest Event: --")
        );
        clearTimer.setRepeats(false);

        // Listen for simplified GPS events
        simplifiedStream.listen(data -> {
            SwingUtilities.invokeLater(() -> {

                // Separate the event into its individual fields
                String[] fields = data.split(",");

                latitude0.setText("Latitude: " + fields[1]);
                longitude0.setText("Longitude: " + fields[2]);

                event0.setText("Latest Event: " + data);

                // Restart the timer whenever a new event arrives
                clearTimer.restart();
            });
        });
    }

    public static void main(String[] args) {
        // Start the GUI on Swing's Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            GpsGUI gui = new GpsGUI();
            gui.setVisible(true);
        });
    }
}