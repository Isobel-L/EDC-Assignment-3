import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import nz.sodium.Stream;
import nz.sodium.CellSink;

public class GpsGUI extends JFrame {

    // Immutable restriction values used by Sodium FRP
    private static class GpsRestrictions {
        final double latitude;
        final double longitude;

        GpsRestrictions(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        boolean contains(GpsEvent event) {
            return Math.abs(event.latitude) <= latitude
                && Math.abs(event.longitude) <= longitude;
        }
    }

    // Validate a coordinate restriction before updating Sodium
    private static double parseRestriction(
            String input, String coordinate, double maximum) {

        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(
                coordinate + " restriction cannot be empty."
            );
        }

        double value;

        try {
            value = Double.parseDouble(input.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                coordinate + " restriction must be a valid number."
            );
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                coordinate + " restriction must be a finite number."
            );
        }

        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException(
                coordinate + " restriction must be between 0 and "
                + maximum + "."
            );
        }

        return value == 0 ? 0.0 : value;
    }

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

        // Combine all GPS event streams into one Sodium stream
        Stream<GpsEvent> combinedStream = streams[0];

        for (int i = 1; i < streams.length; i++) {
            combinedStream = combinedStream.orElse(streams[i]);
        }

        // Create latitude and longitude restriction controls
        JPanel restrictionPanel = new JPanel(new GridLayout(2, 3, 10, 5));

        JTextField latitudeInput = new JTextField("40.0");
        JTextField longitudeInput = new JTextField("116.0");

        JButton setRestrictionButton = new JButton("Set Restriction");

        // Show the actual initial restrictions
        JLabel latitudeRestrictionLabel = new JLabel(
            "Latitude restriction: +/- 90.0"
        );
        JLabel longitudeRestrictionLabel = new JLabel(
            "Longitude restriction: +/- 180.0"
        );

        restrictionPanel.add(new JLabel("Latitude (+/- degrees):"));
        restrictionPanel.add(latitudeInput);
        restrictionPanel.add(latitudeRestrictionLabel);

        restrictionPanel.add(new JLabel("Longitude (+/- degrees):"));
        restrictionPanel.add(longitudeInput);
        restrictionPanel.add(longitudeRestrictionLabel);

        // Keep the restriction button in its own panel
        JPanel restrictionControls = new JPanel(new BorderLayout());
        restrictionControls.add(restrictionPanel, BorderLayout.CENTER);
        restrictionControls.add(
            setRestrictionButton,
            BorderLayout.SOUTH
        );

        restrictionControls.setBorder(
            javax.swing.BorderFactory.createTitledBorder(
                "GPS Coordinate Restrictions"
            )
        );

        // Store current restrictions in a Sodium reactive cell
        // Initially allow the full valid coordinate range
        CellSink<GpsRestrictions> activeRestrictions =
            new CellSink<>(new GpsRestrictions(90.0, 180.0));

        // Update restrictions only after both inputs are valid
        setRestrictionButton.addActionListener(e -> {
            try {
                double latitude = parseRestriction(
                    latitudeInput.getText(), "Latitude", 90.0
                );

                double longitude = parseRestriction(
                    longitudeInput.getText(), "Longitude", 180.0
                );

                // Send both validated values to Sodium together
                activeRestrictions.send(
                    new GpsRestrictions(latitude, longitude)
                );

                latitudeRestrictionLabel.setText(
                    "Latitude restriction: +/- " + latitude
                );
                longitudeRestrictionLabel.setText(
                    "Longitude restriction: +/- " + longitude
                );

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Invalid Restriction",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // Check each event against the currently active restrictions
        Stream<GpsEvent> checkedStream = combinedStream.snapshot(
            activeRestrictions,
            (event, restrictions) ->
                restrictions.contains(event) ? event : null
        );

        // Remove events that do not satisfy the restrictions
        Stream<GpsEvent> filteredStream = checkedStream.filter(
            event -> event != null
        );

        // Create a display for the combined GPS events
        JPanel combinedPanel = new JPanel(new BorderLayout());

        JLabel combinedHeading = new JLabel(
            "Combined GPS Events",
            JLabel.CENTER
        );

        JLabel combinedEventLabel = new JLabel(
            "Latest Event: --",
            JLabel.CENTER
        );

        combinedPanel.add(combinedHeading, BorderLayout.NORTH);
        combinedPanel.add(combinedEventLabel, BorderLayout.CENTER);

        combinedPanel.setBorder(
            javax.swing.BorderFactory.createEtchedBorder()
        );

        // Group the restrictions and combined display
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(restrictionControls, BorderLayout.NORTH);
        bottomPanel.add(combinedPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // Clear combined event after 3 seconds
        Timer combinedTimer = new Timer(3000, e ->
            combinedEventLabel.setText("Latest Event: --")
        );
        combinedTimer.setRepeats(false);

        // Display only events that pass the Sodium filter
        filteredStream.listen(event -> {
            SwingUtilities.invokeLater(() -> {
                combinedEventLabel.setText(
                    "Latest Event: " + event.name + ","
                    + event.latitude + ","
                    + event.longitude + ","
                    + event.altitude
                );

                combinedTimer.restart();
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
