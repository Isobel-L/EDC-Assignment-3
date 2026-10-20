import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.awt.GridLayout;

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
    }

    public static void main(String[] args) {
        // Start the GUI on Swing's Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            GpsGUI gui = new GpsGUI();
            gui.setVisible(true);
        });
    }
}