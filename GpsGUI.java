import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Cursor;

import java.util.ArrayList;
import java.util.List;

import nz.sodium.Stream;
import nz.sodium.StreamSink;
import nz.sodium.Cell;
import nz.sodium.CellSink;
import nz.sodium.Listener;

public class GpsGUI extends JFrame {

    // ---------------------------------------------------------
    // Dashboard colours
    // ---------------------------------------------------------

    private static final Color BACKGROUND = new Color(244, 247, 251);

    private static final Color CARD_BACKGROUND = Color.WHITE;

    private static final Color PRIMARY = new Color(35, 70, 120);

    private static final Color ACCENT = new Color(30, 105, 180);

    private static final Color TEXT = new Color(40, 48, 60);

    private static final Color MUTED = new Color(105, 115, 130);

    private static final Color BORDER = new Color(218, 225, 234);

    private static final Color DISTANCE_BACKGROUND = new Color(231, 242, 253);

    // Button colours
    private static final Color BUTTON_BACKGROUND = new Color(210, 224, 239);

    private static final Color BUTTON_BORDER = new Color(145, 165, 190);

    // ---------------------------------------------------------
    // Fonts
    // ---------------------------------------------------------

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);

    private static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 15);

    private static final Font TRACKER_FONT = new Font("Segoe UI", Font.BOLD, 15);

    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    private static final Font DISTANCE_FONT = new Font("Segoe UI", Font.BOLD, 13);

    // ---------------------------------------------------------
    // Application state
    // ---------------------------------------------------------

    private final List<Listener> listeners = new ArrayList<>();

    private final JLabel[] latitudeLabels = new JLabel[10];

    private final JLabel[] longitudeLabels = new JLabel[10];

    private final JLabel[] eventLabels = new JLabel[10];

    private final JLabel[] distanceLabels = new JLabel[10];

    // ---------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------

    private static JLabel createLabel(
            String text,
            Font font,
            Color colour) {

        JLabel label = new JLabel(text);

        label.setFont(font);
        label.setForeground(colour);

        return label;
    }

    private static JPanel createCard() {

        JPanel card = new JPanel();

        card.setBackground(CARD_BACKGROUND);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(
                                10, 13, 10, 13)));

        return card;
    }

    private static JTextField createInput(
            String initialValue) {

        JTextField field = new JTextField(initialValue);

        field.setFont(BODY_FONT);

        field.setPreferredSize(
                new Dimension(140, 30));

        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(
                                5, 8, 5, 8)));

        return field;
    }

    private static double parseRestriction(
            String input,
            String coordinate,
            double maximum) {

        if (input == null ||
                input.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    coordinate
                            + " restriction cannot be empty.");
        }

        double value;

        try {

            value = Double.parseDouble(
                    input.trim());

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    coordinate
                            + " restriction must be a valid number.");
        }

        if (!Double.isFinite(value)) {

            throw new IllegalArgumentException(
                    coordinate
                            + " restriction must be a finite number.");
        }

        if (value < 0 || value > maximum) {

            throw new IllegalArgumentException(
                    coordinate
                            + " restriction must be between 0 and "
                            + maximum + ".");
        }

        return value == 0 ? 0.0 : value;
    }

    // ---------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------

    public GpsGUI() {

        setTitle("GPS Tracking System");

        setSize(1100, 850);

        setMinimumSize(
                new Dimension(850, 600));

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));

        mainPanel.setBackground(BACKGROUND);

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 15, 12, 15));

        setContentPane(mainPanel);

        // -----------------------------------------------------
        // Header
        // -----------------------------------------------------

        JPanel header = new JPanel(new BorderLayout());

        header.setOpaque(false);

        JLabel title = createLabel(
                "GPS Tracking Dashboard",
                TITLE_FONT,
                PRIMARY);

        JLabel subtitle = createLabel(
                "Live monitoring of 10 GPS trackers",
                BODY_FONT,
                MUTED);

        JPanel titlePanel = new JPanel();

        titlePanel.setOpaque(false);

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS));

        titlePanel.add(title);

        titlePanel.add(
                Box.createVerticalStrut(3));

        titlePanel.add(subtitle);

        header.add(
                titlePanel,
                BorderLayout.WEST);

        JLabel systemStatus = createLabel(
                "LIVE TRACKING",
                DISTANCE_FONT,
                ACCENT);

        header.add(
                systemStatus,
                BorderLayout.EAST);

        mainPanel.add(
                header,
                BorderLayout.NORTH);

        // -----------------------------------------------------
        // Tracker cards
        // -----------------------------------------------------

        JPanel trackerGrid = new JPanel(
                new GridLayout(5, 2, 12, 12));

        trackerGrid.setBackground(BACKGROUND);

        for (int i = 0; i < 10; i++) {

            JPanel card = createCard();

            card.setLayout(
                    new BorderLayout(0, 8));

            JLabel trackerHeading = createLabel(
                    "TRACKER " + i,
                    TRACKER_FONT,
                    PRIMARY);

            card.add(
                    trackerHeading,
                    BorderLayout.NORTH);

            JPanel details = new JPanel();

            details.setOpaque(false);

            details.setLayout(
                    new BoxLayout(
                            details,
                            BoxLayout.Y_AXIS));

            JLabel latitude = createLabel(
                    "Latitude: --",
                    BODY_FONT,
                    TEXT);

            JLabel longitude = createLabel(
                    "Longitude: --",
                    BODY_FONT,
                    TEXT);

            JLabel event = createLabel(
                    "Latest Event: --",
                    BODY_FONT,
                    MUTED);

            details.add(latitude);

            details.add(
                    Box.createVerticalStrut(4));

            details.add(longitude);

            details.add(
                    Box.createVerticalStrut(4));

            details.add(event);

            card.add(
                    details,
                    BorderLayout.CENTER);

            JPanel distancePanel = new JPanel(
                    new FlowLayout(
                            FlowLayout.LEFT, 8, 5));

            distancePanel.setBackground(
                    DISTANCE_BACKGROUND);

            JLabel distance = createLabel(
                    "Distance (last 5 min): 0 m",
                    DISTANCE_FONT,
                    PRIMARY);

            distancePanel.add(distance);

            card.add(
                    distancePanel,
                    BorderLayout.SOUTH);

            trackerGrid.add(card);

            latitudeLabels[i] = latitude;
            longitudeLabels[i] = longitude;
            eventLabels[i] = event;
            distanceLabels[i] = distance;
        }

        JScrollPane trackerScroll = new JScrollPane(trackerGrid);

        trackerScroll.setBorder(null);

        trackerScroll.setBackground(
                BACKGROUND);

        trackerScroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        mainPanel.add(
                trackerScroll,
                BorderLayout.CENTER);

        // -----------------------------------------------------
        // GPS Service and Sodium state
        // -----------------------------------------------------

        GpsService service = new GpsService();

        Stream<GpsEvent>[] streams = service.getEventStreams();

        CellSink<GpsRestrictions> activeRestrictions = new CellSink<>(
                new GpsRestrictions(
                        90.0, 180.0));

        StreamSink<Long> refreshRequests = new StreamSink<>();

        // -----------------------------------------------------
        // Individual tracker processing
        // -----------------------------------------------------

        for (int i = 0; i < 10; i++) {

            final int trackerIndex = i;

            JLabel latitudeLabel = latitudeLabels[i];

            JLabel longitudeLabel = longitudeLabels[i];

            JLabel distanceLabel = distanceLabels[i];

            GpsEventDisplay eventDisplay = new GpsEventDisplay(
                    eventLabels[i]);

            // Remove altitude using Sodium map()
            Stream<String> simplifiedStream = streams[i].map(event -> event.name + ","
                    + event.latitude + ","
                    + event.longitude);

            listeners.add(
                    simplifiedStream.listen(data -> {

                        SwingUtilities.invokeLater(() -> {

                            String[] fields = data.split(",");

                            latitudeLabel.setText(
                                    "Latitude: " + fields[1]);

                            longitudeLabel.setText(
                                    "Longitude: " + fields[2]);

                            eventDisplay.showEvent(data);
                        });
                    }));

            // Convert altitude from feet to metres
            Stream<GpsEvent> convertedStream = streams[i].map(event -> new GpsEvent(
                    event.name,
                    event.latitude,
                    event.longitude,
                    GpsDistance.feetToMetres(
                            event.altitude)));

            // Attach arrival timestamps
            Stream<GpsHistory.TimedEvent> timestampedStream = convertedStream.map(event -> new GpsHistory.TimedEvent(
                    event,
                    System.currentTimeMillis()));

            // Maintain reactive five-minute history
            Cell<GpsHistory> historyCell = timestampedStream.accum(
                    new GpsHistory(),
                    (event, previousHistory) -> previousHistory.add(event));

            // React to history or restriction changes
            Cell<Long> distanceCell = historyCell.lift(
                    activeRestrictions,
                    (history, restrictions) -> history.getRoundedDistance(
                            System.currentTimeMillis(),
                            restrictions));

            listeners.add(
                    distanceCell.listen(distance -> {

                        SwingUtilities.invokeLater(() -> {

                            distanceLabel.setText(
                                    "Distance (last 5 min): "
                                            + distance + " m");
                        });
                    }));

            // Refresh distances as old events expire
            Stream<Long> distanceFromRefresh = refreshRequests.snapshot(
                    historyCell,
                    activeRestrictions,
                    (currentTime,
                            history,
                            restrictions) -> history.getRoundedDistance(
                                    currentTime,
                                    restrictions));

            listeners.add(
                    distanceFromRefresh.listen(
                            distance -> {

                                SwingUtilities.invokeLater(() -> {

                                    distanceLabels[trackerIndex].setText(
                                            "Distance (last 5 min): "
                                                    + distance + " m");
                                });
                            }));
        }

        // -----------------------------------------------------
        // Combined GPS stream
        // -----------------------------------------------------

        Stream<GpsEvent> combinedStream = streams[0];

        for (int i = 1; i < streams.length; i++) {

            combinedStream = combinedStream.orElse(
                    streams[i]);
        }

        // -----------------------------------------------------
        // Bottom dashboard section
        // -----------------------------------------------------

        JPanel bottomSection = new JPanel(
                new BorderLayout(0, 10));

        bottomSection.setOpaque(false);

        // -----------------------------------------------------
        // Restriction controls
        // -----------------------------------------------------

        JPanel restrictionCard = createCard();

        restrictionCard.setLayout(
                new BorderLayout(0, 10));

        JLabel restrictionHeading = createLabel(
                "Coordinate Restrictions",
                SECTION_FONT,
                PRIMARY);

        restrictionCard.add(
                restrictionHeading,
                BorderLayout.NORTH);

        JPanel controls = new JPanel(
                new GridLayout(2, 3, 12, 8));

        controls.setOpaque(false);

        JTextField latitudeInput = createInput("90.0");

        JTextField longitudeInput = createInput("180.0");

        JLabel latitudeRestrictionLabel = createLabel(
                "Active: +/- 90.0",
                BODY_FONT,
                MUTED);

        JLabel longitudeRestrictionLabel = createLabel(
                "Active: +/- 180.0",
                BODY_FONT,
                MUTED);

        controls.add(
                createLabel(
                        "Latitude (+/- degrees)",
                        BODY_FONT,
                        TEXT));

        controls.add(latitudeInput);
        controls.add(latitudeRestrictionLabel);

        controls.add(
                createLabel(
                        "Longitude (+/- degrees)",
                        BODY_FONT,
                        TEXT));

        controls.add(longitudeInput);
        controls.add(longitudeRestrictionLabel);

        restrictionCard.add(
                controls,
                BorderLayout.CENTER);

        // -----------------------------------------------------
        // Apply Restrictions button
        // -----------------------------------------------------

        JButton applyButton = new JButton("Apply Restrictions");

        applyButton.setFont(DISTANCE_FONT);

        // Soft blue-grey background
        applyButton.setBackground(
                BUTTON_BACKGROUND);

        // Dark blue text
        applyButton.setForeground(
                PRIMARY);

        // Visible border with comfortable padding
        applyButton.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BUTTON_BORDER),
                        BorderFactory.createEmptyBorder(
                                8, 16, 8, 16)));

        // Ensure our custom background is displayed
        applyButton.setContentAreaFilled(false);
        applyButton.setOpaque(true);

        applyButton.setFocusPainted(false);

        applyButton.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        applyButton.setPreferredSize(
                new Dimension(180, 38));

        restrictionCard.add(
                applyButton,
                BorderLayout.SOUTH);

        bottomSection.add(
                restrictionCard,
                BorderLayout.NORTH);

        // -----------------------------------------------------
        // Combined event panel
        // -----------------------------------------------------

        JPanel combinedCard = createCard();

        combinedCard.setLayout(
                new BorderLayout(0, 7));

        JLabel combinedHeading = createLabel(
                "Combined GPS Event Stream",
                SECTION_FONT,
                PRIMARY);

        JLabel combinedEventLabel = createLabel(
                "Latest Event: --",
                BODY_FONT,
                TEXT);

        combinedCard.add(
                combinedHeading,
                BorderLayout.NORTH);

        combinedCard.add(
                combinedEventLabel,
                BorderLayout.CENTER);

        bottomSection.add(
                combinedCard,
                BorderLayout.CENTER);

        mainPanel.add(
                bottomSection,
                BorderLayout.SOUTH);

        GpsEventDisplay combinedEventDisplay = new GpsEventDisplay(
                combinedEventLabel);

        // -----------------------------------------------------
        // Apply coordinate restrictions
        // -----------------------------------------------------

        applyButton.addActionListener(e -> {

            try {

                double latitude = parseRestriction(
                        latitudeInput.getText(),
                        "Latitude",
                        90.0);

                double longitude = parseRestriction(
                        longitudeInput.getText(),
                        "Longitude",
                        180.0);

                GpsRestrictions newRestrictions = new GpsRestrictions(
                        latitude,
                        longitude);

                activeRestrictions.send(
                        newRestrictions);

                latitudeRestrictionLabel.setText(
                        "Active: +/- " + latitude);

                longitudeRestrictionLabel.setText(
                        "Active: +/- " + longitude);

                refreshRequests.send(
                        System.currentTimeMillis());

                combinedEventDisplay.clear();

            } catch (IllegalArgumentException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Invalid Restriction",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // -----------------------------------------------------
        // Filter combined events using Sodium
        // -----------------------------------------------------

        Stream<GpsEvent> checkedStream = combinedStream.snapshot(
                activeRestrictions,
                (event, restrictions) -> restrictions.contains(event)
                        ? event
                        : null);

        Stream<GpsEvent> filteredStream = checkedStream.filter(
                event -> event != null);

        listeners.add(
                filteredStream.listen(event -> {

                    SwingUtilities.invokeLater(() -> {

                        String eventText = event.name + ","
                                + event.latitude + ","
                                + event.longitude + ","
                                + event.altitude;

                        combinedEventDisplay.showEvent(
                                eventText);
                    });
                }));

        // -----------------------------------------------------
        // Refresh five-minute distances every second
        // -----------------------------------------------------

        Timer distanceRefreshTimer = new Timer(
                1000,
                e -> refreshRequests.send(
                        System.currentTimeMillis()));

        distanceRefreshTimer.start();
    }

    // ---------------------------------------------------------
    // Main
    // ---------------------------------------------------------

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                UIManager.setLookAndFeel(
                        UIManager
                                .getSystemLookAndFeelClassName());

            } catch (Exception ignored) {
                // Use the default Swing appearance
            }

            GpsGUI gui = new GpsGUI();

            gui.setVisible(true);
        });
    }
}