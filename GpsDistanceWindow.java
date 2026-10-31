import java.util.ArrayDeque;
import java.util.Deque;

public class GpsDistanceWindow {

    // Five-minute window in milliseconds
    private static final long WINDOW_DURATION = 5 * 60 * 1000L;

    // Store a GPS event and the time it was received
    private static class TimedGpsEvent {

        final GpsEvent event;
        final long timestamp;

        TimedGpsEvent(GpsEvent event, long timestamp) {
            this.event = event;
            this.timestamp = timestamp;
        }
    }

    // Store GPS positions in chronological order
    private final Deque<TimedGpsEvent> events = new ArrayDeque<>();

    // Remember the most recent event
    private TimedGpsEvent previousEvent = null;

    // Store the current restrictions
    private GpsRestrictions currentRestrictions =
        new GpsRestrictions(90.0, 180.0);

    // Record whether altitude is already in metres
    private final boolean altitudeInMetres;

    // Default constructor: original GPS altitude is in feet
    public GpsDistanceWindow() {
        this(false);
    }

    // Constructor for streams with altitude already converted
    public GpsDistanceWindow(boolean altitudeInMetres) {
        this.altitudeInMetres = altitudeInMetres;
    }

    // Process a new GPS event
    public void addEvent(GpsEvent event, long timestamp) {

        // Ignore events arriving out of timestamp order
        if (previousEvent != null
                && timestamp < previousEvent.timestamp) {
            return;
        }

        // Remove GPS events outside the five-minute window
        removeExpiredEvents(timestamp);

        // Store the new GPS event
        TimedGpsEvent timedEvent = new TimedGpsEvent(event, timestamp);

        events.addLast(timedEvent);

        // Remember the latest position
        previousEvent = timedEvent;
    }

    // Remove GPS events older than five minutes
    private void removeExpiredEvents(long currentTime) {

        long cutoff = currentTime - WINDOW_DURATION;

        while (!events.isEmpty()
                && events.peekFirst().timestamp < cutoff) {

            events.removeFirst();
        }
    }

    // Calculate distance using current restrictions
    public double getDistance(long currentTime) {

        return getDistance(currentTime, currentRestrictions);
    }

    // Calculate distance between accepted GPS positions
    public double getDistance(
            long currentTime, GpsRestrictions restrictions) {

        removeExpiredEvents(currentTime);

        double totalDistance = 0.0;

        TimedGpsEvent previousAccepted = null;

        for (TimedGpsEvent current : events) {

            // Skip positions outside the active restrictions
            if (!restrictions.contains(current.event)) {
                continue;
            }

            if (previousAccepted != null) {

                long timeDifference =
                    current.timestamp - previousAccepted.timestamp;

                if (timeDifference >= 0
                        && timeDifference <= WINDOW_DURATION) {

                    double distance;

                    if (altitudeInMetres) {

                        // Altitude was converted using Sodium map()
                        distance = GpsDistance.calculateDistanceMetres(
                            previousAccepted.event,
                            current.event
                        );

                    } else {

                        // Original GPS altitude remains in feet
                        distance = GpsDistance.calculateDistance(
                            previousAccepted.event,
                            current.event
                        );
                    }

                    totalDistance += distance;
                }
            }

            previousAccepted = current;
        }

        return totalDistance;
    }

    // Return distance rounded up to the nearest metre
    public long getRoundedDistance(long currentTime) {

        return (long) Math.ceil(getDistance(currentTime));
    }

    // Return rounded distance using selected restrictions
    public long getRoundedDistance(
            long currentTime, GpsRestrictions restrictions) {

        return (long) Math.ceil(
            getDistance(currentTime, restrictions)
        );
    }

    // Update current restrictions
    public void setRestrictions(GpsRestrictions restrictions) {

        currentRestrictions = restrictions;
    }

    // Reset distance history
    public void reset() {

        events.clear();

        previousEvent = null;
    }
}