import java.util.ArrayDeque;
import java.util.Deque;

public class GpsDistanceWindow {

    // Five-minute window in milliseconds
    private static final long WINDOW_DURATION = 5 * 60 * 1000L;

    // Store distances between consecutive GPS events
    private static class DistanceSegment {

        final long timestamp;
        final double distance;

        DistanceSegment(long timestamp, double distance) {
            this.timestamp = timestamp;
            this.distance = distance;
        }
    }

    private final Deque<DistanceSegment> segments = new ArrayDeque<>();

    // Store the previous GPS position
    private GpsEvent previousEvent = null;

    // Track when the previous event arrived
    private long previousTimestamp = 0;

    // Store the running total distance
    private double totalDistance = 0.0;

    // Process a new GPS event
    public void addEvent(GpsEvent event, long timestamp) {

        // Remove movements older than five minutes
        removeExpiredSegments(timestamp);

        // Calculate distance from the previous position
        if (previousEvent != null) {

            // Only connect positions within the five-minute window
            if (timestamp >= previousTimestamp
                    && timestamp - previousTimestamp <= WINDOW_DURATION) {

                double distance = GpsDistance.calculateDistance(
                    previousEvent,
                    event
                );

                // Store the movement and its timestamp
                segments.addLast(
                    new DistanceSegment(timestamp, distance)
                );

                totalDistance += distance;
            }
        }

        // Remember the current position
        previousEvent = event;
        previousTimestamp = timestamp;
    }

    // Remove movements outside the five-minute window
    private void removeExpiredSegments(long currentTime) {

        long cutoff = currentTime - WINDOW_DURATION;

        while (!segments.isEmpty()
                && segments.peekFirst().timestamp < cutoff) {

            DistanceSegment expired = segments.removeFirst();

            totalDistance -= expired.distance;
        }

        // Prevent small negative values from floating-point rounding
        totalDistance = Math.max(0.0, totalDistance);
    }

    // Return the current distance in metres
    public double getDistance(long currentTime) {

        removeExpiredSegments(currentTime);

        return totalDistance;
    }

    // Return distance rounded up to the nearest metre
    public long getRoundedDistance(long currentTime) {

        return (long) Math.ceil(getDistance(currentTime));
    }

    // Reset the window
    public void reset() {

        segments.clear();

        previousEvent = null;
        previousTimestamp = 0;

        totalDistance = 0.0;
    }
}