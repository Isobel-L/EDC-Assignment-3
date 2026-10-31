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

    private final List<Listener> listeners = new ArrayList<>();

    private final JLabel[] distanceLabels = new JLabel[10];

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

        setTitle("GPS Tracking System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel heading = new JLabel(
            "GPS Tracking System",
            JLabel.CENTER
        );

        add(heading, BorderLayout.NORTH);

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

        GpsService service = new GpsService();
        Stream<GpsEvent>[] streams = service.getEventStreams();

        CellSink<GpsRestrictions> activeRestrictions =
            new CellSink<>(
                new GpsRestrictions(90.0, 180.0)
            );

        StreamSink<Long> refreshRequests = new StreamSink<>();

        // Process each of the ten GPS tracker streams
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

            // Use the separately tested event display class
            GpsEventDisplay eventDisplay =
                new GpsEventDisplay(eventLabel);

            // Remove altitude from simplified events using Sodium
            Stream<String> simplifiedStream = streams[i].map(
                event ->
                    event.name + ","
                    + event.latitude + ","
                    + event.longitude
            );

            listeners.add(simplifiedStream.listen(data -> {

                SwingUtilities.invokeLater(() -> {

                    String[] fields = data.split(",");

                    latitudeLabel.setText(
                        "Latitude: " + fields[1]
                    );

                    longitudeLabel.setText(
                        "Longitude: " + fields[2]
                    );

                    // The display class handles the timer
                    eventDisplay.showEvent(data);
                });
            }));

            // Convert altitude from feet to metres using Sodium map()
            Stream<GpsEvent> convertedStream = streams[i].map(
                event ->
                    new GpsEvent(
                        event.name,
                        event.latitude,
                        event.longitude,
                        GpsDistance.feetToMetres(event.altitude)
                    )
            );

            // Attach event timestamps using Sodium map()
            Stream<GpsHistory.TimedEvent> timestampedStream =
                convertedStream.map(
                    event ->
                        new GpsHistory.TimedEvent(
                            event,
                            System.currentTimeMillis()
                        )
                );

            // Maintain immutable GPS history using Sodium accum()
            Cell<GpsHistory> historyCell =
                timestampedStream.accum(
                    new GpsHistory(),
                    (event, previousHistory) ->
                        previousHistory.add(event)
                );

            // Recalculate distance when history or restrictions change
            Cell<Long> distanceCell = historyCell.lift(
                activeRestrictions,
                (history, restrictions) ->
                    history.getRoundedDistance(
                        System.currentTimeMillis(),
                        restrictions
                    )
            );

            listeners.add(distanceCell.listen(distance -> {

                SwingUtilities.invokeLater(() -> {

                    distanceLabels[trackerIndex].setText(
                        "Distance (last 5 min): "
                        + distance + " m"
                    );
                });
            }));

            // Refresh distance when older positions expire
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

            listeners.add(distanceFromRefresh.listen(distance -> {

                SwingUtilities.invokeLater(() -> {

                    distanceLabels[trackerIndex].setText(
                        "Distance (last 5 min): "
                        + distance + " m"
                    );
                });
            }));
        }

        // Combine the ten tracker event streams
        Stream<GpsEvent> combinedStream = streams[0];

        for (int i = 1; i < streams.length; i++) {
            combinedStream = combinedStream.orElse(streams[i]);
        }

        // Create restriction controls
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

        // Combined GPS event display
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

        // Use the same tested timer implementation
        // for the combined event display
        GpsEventDisplay combinedEventDisplay =
            new GpsEventDisplay(combinedEventLabel);

        // Apply coordinate restrictions
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

                activeRestrictions.send(newRestrictions);

                latitudeRestrictionLabel.setText(
                    "Latitude restriction: +/- " + latitude
                );

                longitudeRestrictionLabel.setText(
                    "Longitude restriction: +/- " + longitude
                );

                refreshRequests.send(
                    System.currentTimeMillis()
                );

                // Clear the combined display whenever
                // the coordinate restrictions change
                combinedEventDisplay.clear();

            } catch (IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Invalid Restriction",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // Filter combined events using current restrictions
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

                String eventText =
                    event.name + ","
                    + event.latitude + ","
                    + event.longitude + ","
                    + event.altitude;

                combinedEventDisplay.showEvent(eventText);
            });
        }));

        // Recalculate five-minute distances every second
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