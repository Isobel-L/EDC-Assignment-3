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

    // Remember the most recent event for boundary calculations
    private TimedGpsEvent previousEvent = null;

    // Store the current restrictions
    private GpsRestrictions currentRestrictions =
        new GpsRestrictions(90.0, 180.0);

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

    // Remove GPS events older than the five-minute window
    private void removeExpiredEvents(long currentTime) {

        long cutoff = currentTime - WINDOW_DURATION;

        while (!events.isEmpty()
                && events.peekFirst().timestamp < cutoff) {

            events.removeFirst();
        }
    }

    // Calculate the current total distance
    public double getDistance(long currentTime) {

        return getDistance(currentTime, currentRestrictions);
    }

    // Calculate distance using the selected restrictions
    public double getDistance(
            long currentTime, GpsRestrictions restrictions) {

        removeExpiredEvents(currentTime);

        double totalDistance = 0.0;

        TimedGpsEvent previousAccepted = null;

        // Process only GPS events within the selected restrictions
        for (TimedGpsEvent current : events) {

            if (!restrictions.contains(current.event)) {
                continue;
            }

            // Calculate movement from the previous accepted position
            if (previousAccepted != null) {

                long timeDifference =
                    current.timestamp - previousAccepted.timestamp;

                // Only connect positions up to five minutes apart
                if (timeDifference >= 0
                        && timeDifference <= WINDOW_DURATION) {

                    totalDistance += GpsDistance.calculateDistance(
                        previousAccepted.event,
                        current.event
                    );
                }
            }

            previousAccepted = current;
        }

        return totalDistance;
    }

    // Return the distance rounded up to the nearest metre
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

    // Update the currently selected restrictions
    public void setRestrictions(GpsRestrictions restrictions) {

        currentRestrictions = restrictions;
    }

    // Reset the window
    public void reset() {

        events.clear();

        previousEvent = null;
    }
}