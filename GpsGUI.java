import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import nz.sodium.Stream;
import nz.sodium.StreamSink;
import nz.sodium.Cell;
import nz.sodium.CellSink;
import nz.sodium.Listener;

public class GpsGUI extends JFrame {

    // Keep Sodium listeners active while the GUI is running
    private final List<Listener> listeners = new ArrayList<>();

    // Display the calculated distance for each tracker
    private final JLabel[] distanceLabels = new JLabel[10];

    // Validate coordinate restrictions
    private static double parseRestriction(
            String input,
            String coordinate,
            double maximum) {

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

        // Main application window
        setTitle("GPS Tracking System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel heading = new JLabel(
            "GPS Tracking System",
            JLabel.CENTER
        );

        add(heading, BorderLayout.NORTH);

        // Create the ten tracker displays
        JPanel trackerPanel = new JPanel(
            new GridLayout(5, 2, 10, 10)
        );

        for (int i = 0; i < 10; i++) {

            JPanel individualTracker = new JPanel(
                new GridLayout(5, 1)
            );

            JLabel nameLabel = new JLabel("Tracker" + i);
            JLabel latitudeLabel = new JLabel("Latitude: --");
            JLabel longitudeLabel = new JLabel("Longitude: --");
            JLabel eventLabel = new JLabel("Latest Event: --");

            JLabel distanceLabel = new JLabel(
                "Distance (last 5 min): 0 m"
            );

            individualTracker.add(nameLabel);
            individualTracker.add(latitudeLabel);
            individualTracker.add(longitudeLabel);
            individualTracker.add(eventLabel);
            individualTracker.add(distanceLabel);

            individualTracker.setBorder(
                BorderFactory.createEtchedBorder()
            );

            trackerPanel.add(individualTracker);

            distanceLabels[i] = distanceLabel;
        }

        add(trackerPanel, BorderLayout.CENTER);

        // Obtain the GPS event streams
        GpsService service = new GpsService();

        Stream<GpsEvent>[] streams = service.getEventStreams();

        // Reactive coordinate restrictions
        CellSink<GpsRestrictions> activeRestrictions =
            new CellSink<>(
                new GpsRestrictions(90.0, 180.0)
            );

        // Reactive refresh requests
        // These allow distances to expire without new GPS events
        StreamSink<Long> refreshRequests = new StreamSink<>();

        // Create the reactive processing pipeline for each tracker
        for (int i = 0; i < 10; i++) {

            final int trackerIndex = i;

            JPanel individualTracker =
                (JPanel) trackerPanel.getComponent(i);

            JLabel latitudeLabel =
                (JLabel) individualTracker.getComponent(1);

            JLabel longitudeLabel =
                (JLabel) individualTracker.getComponent(2);

            JLabel eventLabel =
                (JLabel) individualTracker.getComponent(3);

            // ----------------------------------------------------
            // Individual simplified GPS display
            // ----------------------------------------------------

            // Remove altitude using Sodium map()
            Stream<String> simplifiedStream = streams[i].map(
                event ->
                    event.name + ","
                    + event.latitude + ","
                    + event.longitude
            );

            Timer clearTimer = new Timer(
                3000,
                e -> eventLabel.setText("Latest Event: --")
            );

            clearTimer.setRepeats(false);

            listeners.add(simplifiedStream.listen(data -> {

                SwingUtilities.invokeLater(() -> {

                    String[] fields = data.split(",");

                    latitudeLabel.setText(
                        "Latitude: " + fields[1]
                    );

                    longitudeLabel.setText(
                        "Longitude: " + fields[2]
                    );

                    eventLabel.setText(
                        "Latest Event: " + data
                    );

                    clearTimer.restart();
                });
            }));

            // ----------------------------------------------------
            // FRP distance processing
            // ----------------------------------------------------

            // Step 1: Convert altitude from feet to metres
            // using Sodium map()
            Stream<GpsEvent> convertedStream = streams[i].map(
                event ->
                    new GpsEvent(
                        event.name,
                        event.latitude,
                        event.longitude,
                        GpsDistance.feetToMetres(event.altitude)
                    )
            );

            // Step 2: Attach the arrival time to each GPS event
            // using Sodium map()
            Stream<GpsHistory.TimedEvent> timestampedStream =
                convertedStream.map(
                    event ->
                        new GpsHistory.TimedEvent(
                            event,
                            System.currentTimeMillis()
                        )
                );

            // Step 3: Maintain immutable GPS history
            // using Sodium accum()
            Cell<GpsHistory> historyCell =
                timestampedStream.accum(
                    new GpsHistory(),
                    (event, previousHistory) ->
                        previousHistory.add(event)
                );

            // Step 4: Combine the GPS history and current
            // restrictions using Sodium lift()
            //
            // This recalculates distance whenever either
            // the GPS history or restrictions change.
            Cell<Long> distanceCell = historyCell.lift(
                activeRestrictions,
                (history, restrictions) ->
                    history.getRoundedDistance(
                        System.currentTimeMillis(),
                        restrictions
                    )
            );

            // Step 5: Display reactive distance updates
            listeners.add(distanceCell.listen(distance -> {

                SwingUtilities.invokeLater(() -> {

                    distanceLabels[trackerIndex].setText(
                        "Distance (last 5 min): "
                        + distance + " m"
                    );
                });
            }));

            // Step 6: Recalculate distance every second
            // using Sodium snapshot()
            //
            // This ensures old GPS positions stop
            // contributing to the five-minute distance.
            Stream<Long> distanceFromRefresh =
                refreshRequests.snapshot(
                    historyCell,
                    activeRestrictions,
                    (currentTime, history, restrictions) ->
                        history.getRoundedDistance(
                            currentTime,
                            restrictions
                        )
                );

            // Step 7: Display periodically refreshed distances
            listeners.add(distanceFromRefresh.listen(distance -> {

                SwingUtilities.invokeLater(() -> {

                    distanceLabels[trackerIndex].setText(
                        "Distance (last 5 min): "
                        + distance + " m"
                    );
                });
            }));
        }

        // --------------------------------------------------------
        // Combined GPS event stream
        // --------------------------------------------------------

        Stream<GpsEvent> combinedStream = streams[0];

        for (int i = 1; i < streams.length; i++) {
            combinedStream = combinedStream.orElse(streams[i]);
        }

        // --------------------------------------------------------
        // Coordinate restriction controls
        // --------------------------------------------------------

        JPanel restrictionPanel = new JPanel(
            new GridLayout(2, 3, 10, 5)
        );

        JTextField latitudeInput = new JTextField("90.0");
        JTextField longitudeInput = new JTextField("180.0");

        JButton setRestrictionButton =
            new JButton("Set Restriction");

        JLabel latitudeRestrictionLabel = new JLabel(
            "Latitude restriction: +/- 90.0"
        );

        JLabel longitudeRestrictionLabel = new JLabel(
            "Longitude restriction: +/- 180.0"
        );

        restrictionPanel.add(
            new JLabel("Latitude (+/- degrees):")
        );

        restrictionPanel.add(latitudeInput);
        restrictionPanel.add(latitudeRestrictionLabel);

        restrictionPanel.add(
            new JLabel("Longitude (+/- degrees):")
        );

        restrictionPanel.add(longitudeInput);
        restrictionPanel.add(longitudeRestrictionLabel);

        JPanel restrictionControls = new JPanel(
            new BorderLayout()
        );

        restrictionControls.add(
            restrictionPanel,
            BorderLayout.CENTER
        );

        restrictionControls.add(
            setRestrictionButton,
            BorderLayout.SOUTH
        );

        restrictionControls.setBorder(
            BorderFactory.createTitledBorder(
                "GPS Coordinate Restrictions"
            )
        );

        // --------------------------------------------------------
        // Combined event display
        // --------------------------------------------------------

        JPanel combinedPanel = new JPanel(
            new BorderLayout()
        );

        JLabel combinedHeading = new JLabel(
            "Combined GPS Events",
            JLabel.CENTER
        );

        JLabel combinedEventLabel = new JLabel(
            "Latest Event: --",
            JLabel.CENTER
        );

        combinedPanel.add(
            combinedHeading,
            BorderLayout.NORTH
        );

        combinedPanel.add(
            combinedEventLabel,
            BorderLayout.CENTER
        );

        combinedPanel.setBorder(
            BorderFactory.createEtchedBorder()
        );

        JPanel bottomPanel = new JPanel(
            new BorderLayout()
        );

        bottomPanel.add(
            restrictionControls,
            BorderLayout.NORTH
        );

        bottomPanel.add(
            combinedPanel,
            BorderLayout.CENTER
        );

        add(bottomPanel, BorderLayout.SOUTH);

        Timer combinedTimer = new Timer(
            3000,
            e -> combinedEventLabel.setText("Latest Event: --")
        );

        combinedTimer.setRepeats(false);

        // --------------------------------------------------------
        // Apply coordinate restrictions
        // --------------------------------------------------------

        setRestrictionButton.addActionListener(e -> {

            try {

                double latitude = parseRestriction(
                    latitudeInput.getText(),
                    "Latitude",
                    90.0
                );

                double longitude = parseRestriction(
                    longitudeInput.getText(),
                    "Longitude",
                    180.0
                );

                GpsRestrictions newRestrictions =
                    new GpsRestrictions(
                        latitude,
                        longitude
                    );

                // Update reactive restrictions
                activeRestrictions.send(newRestrictions);

                latitudeRestrictionLabel.setText(
                    "Latitude restriction: +/- " + latitude
                );

                longitudeRestrictionLabel.setText(
                    "Longitude restriction: +/- " + longitude
                );

                // Trigger a refresh using current restrictions
                refreshRequests.send(
                    System.currentTimeMillis()
                );

                // Clear events displayed under old restrictions
                combinedTimer.stop();

                combinedEventLabel.setText(
                    "Latest Event: --"
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

        // --------------------------------------------------------
        // Filter combined GPS events using Sodium
        // --------------------------------------------------------

        Stream<GpsEvent> checkedStream =
            combinedStream.snapshot(
                activeRestrictions,
                (event, restrictions) ->
                    restrictions.contains(event)
                        ? event
                        : null
            );

        Stream<GpsEvent> filteredStream =
            checkedStream.filter(
                event -> event != null
            );

        listeners.add(filteredStream.listen(event -> {

            SwingUtilities.invokeLater(() -> {

                combinedEventLabel.setText(
                    "Latest Event: "
                    + event.name + ","
                    + event.latitude + ","
                    + event.longitude + ","
                    + event.altitude
                );

                combinedTimer.restart();
            });
        }));

        // --------------------------------------------------------
        // Periodic FRP distance refresh
        // --------------------------------------------------------

        // Send the current time through Sodium every second.
        //
        // This triggers snapshot() for all ten trackers,
        // allowing expired positions to stop contributing
        // to the five-minute distance.
        Timer distanceRefreshTimer = new Timer(
            1000,
            e -> refreshRequests.send(
                System.currentTimeMillis()
            )
        );

        distanceRefreshTimer.start();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            GpsGUI gui = new GpsGUI();

            gui.setVisible(true);
        });
    }
}