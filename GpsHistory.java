import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GpsHistory {

    // Five-minute sliding window in milliseconds
    private static final long WINDOW_DURATION = 5 * 60 * 1000L;

    // An immutable timestamped GPS position
    public static class TimedEvent {

        public final GpsEvent event;
        public final long timestamp;

        public TimedEvent(GpsEvent event, long timestamp) {
            this.event = event;
            this.timestamp = timestamp;
        }
    }

    // Store GPS events without allowing external modification
    private final List<TimedEvent> events;

    public GpsHistory() {
        this.events = Collections.emptyList();
    }

    private GpsHistory(List<TimedEvent> events) {
        this.events = Collections.unmodifiableList(events);
    }

    // Return a new history containing the incoming GPS event
    public GpsHistory add(TimedEvent incoming) {

        List<TimedEvent> updated = new ArrayList<>();

        long cutoff = incoming.timestamp - WINDOW_DURATION;

        // Keep GPS positions within the last five minutes
        for (TimedEvent previous : events) {
            if (previous.timestamp >= cutoff) {
                updated.add(previous);
            }
        }

        updated.add(incoming);

        return new GpsHistory(updated);
    }

    // Calculate distance using current restrictions
    public double calculateDistance(
            long currentTime,
            GpsRestrictions restrictions) {

        long cutoff = currentTime - WINDOW_DURATION;

        double totalDistance = 0.0;
        TimedEvent previousAccepted = null;

        for (TimedEvent current : events) {

            if (current.timestamp < cutoff) {
                continue;
            }

            if (!restrictions.contains(current.event)) {
                continue;
            }

            if (previousAccepted != null) {

                long gap =
                    current.timestamp - previousAccepted.timestamp;

                if (gap >= 0 && gap <= WINDOW_DURATION) {

                    // Altitude was already converted with Sodium map()
                    totalDistance += GpsDistance.calculateDistanceMetres(
                        previousAccepted.event,
                        current.event
                    );
                }
            }

            previousAccepted = current;
        }

        return totalDistance;
    }

    public long getRoundedDistance(
            long currentTime,
            GpsRestrictions restrictions) {

        return (long) Math.ceil(
            calculateDistance(currentTime, restrictions)
        );
    }

    public int size() {
        return events.size();
    }
}