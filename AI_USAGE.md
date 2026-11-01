# AI Usage Documentation

**Course:** Event-Driven Computing  
**Assignment:** Programming Assignment 3 – GPS Tracking System  
**AI Tool:** OpenAI ChatGPT (GPT-6)

## 1. Overview
I used ChatGPT throughout this assignment to help me understand Functional Reactive Programming (FRP), work through implementation challenges, and debug errors in my Java code. Most of my interactions involved asking for explanations of concepts, discussing how to approach particular requirements, and getting help when parts of the implementation were not working as expected.

ChatGPT also provided example code and suggested changes to existing Java files. These suggestions were incorporated into the application and checked through local compilation, JUnit testing, and manual testing of the graphical interface.

The following sections describe specific areas where AI assistance was used, including examples of code that was discussed or developed with ChatGPT.

## 2. Understanding Functional Reactive Programming
One of the main reasons I used ChatGPT was to improve my understanding of Sodium and how FRP differs from conventional event-driven Java programming.

I initially found it difficult to understand how the individual GPS streams should be combined, how restrictions could be updated dynamically, and how distance calculations could be maintained reactively.

ChatGPT helped explain the purpose of Sodium operations such as `map()`, `orElse()` and `snapshot()`, including how they relate to one another.

### Example 1: Combining GPS Event Streams
In `GpsGUI.java`, the ten individual GPS streams are combined using:
-------------------------------------------------
Stream<GpsEvent> combinedStream = streams[0];

for (int i = 1; i < streams.length; i++) {
    combinedStream = combinedStream.orElse(streams[i]);
}
-------------------------------------------------
ChatGPT helped explain how `orElse()` combines multiple event streams so that the application can process GPS events from different trackers through a single stream. This was useful when implementing the combined GPS display and understanding why the application needed both individual and combined event processing.

### Example 2: Applying Restrictions Using `snapshot()`
Another concept I needed help understanding was how to apply the current latitude and longitude restrictions to incoming GPS events.
ChatGPT explained how `snapshot()` allows an event stream to access the current value of a reactive cell.

The implementation uses:
-------------------------------------------------
Stream<GpsEvent> checkedStream =
    combinedStream.snapshot(
        activeRestrictions,
        (event, restrictions) ->
            restrictions.contains(event)
                ? event
                : null
    );

Stream<GpsEvent> filteredStream =
    checkedStream.filter(event -> event != null);
-------------------------------------------------
This helped me understand the relationship between incoming events and the current restrictions.
I also tested the behaviour manually by changing latitude and longitude limits in the GUI and checking which trackers remained eligible.

## 3. Understanding and Implementing Distance Calculations
I also used ChatGPT to help understand how GPS coordinates could be used to calculate the distance travelled by each tracker.
This included discussing the difference between horizontal distance and three-dimensional distance, converting altitude from feet to metres, and calculating distance over a five-minute window.

### Example 3: Converting Altitude
The GPS data provides altitude in feet, while the distance calculation uses metres.
ChatGPT helped explain why this conversion was necessary and how Sodium's `map()` operation could perform it before calculating distances.

The conversion method in `GpsDistance.java` is:
-------------------------------------------------
public static double feetToMetres(double feet) {
    return feet * 0.3048;
}
-------------------------------------------------

In `GpsGUI.java`, this is applied to incoming events:
-------------------------------------------------
Stream<GpsEvent> convertedStream = streams[i].map(
    event ->
        new GpsEvent(
            event.name,
            event.latitude,
            event.longitude,
            GpsDistance.feetToMetres(event.altitude)
        )
);
-------------------------------------------------


### Example 4: Combining Reactive Values Using `lift()`
ChatGPT also helped explain how the current GPS history and coordinate restrictions could be combined to calculate distances.

The implementation uses:
-------------------------------------------------
Cell<Long> distanceCell = historyCell.lift(
    activeRestrictions,
    (history, restrictions) ->
        history.getRoundedDistance(
            System.currentTimeMillis(),
            restrictions
        )
);
-------------------------------------------------
This allowed the displayed distance to respond to changes in either the GPS history or the active restrictions.
I verified this behaviour manually by setting both restrictions to zero and then restoring their original values. The distances were recalculated using the retained GPS history.

## 5. Debugging and Resolving Errors
Debugging was one of the areas where I used ChatGPT most frequently.
When I encountered errors, I provided the terminal output and asked ChatGPT to explain what was causing the issue and how it could be resolved.
This was particularly useful when working with Sodium because some methods were not accessible in the version supplied for the assignment.

### Example 5: Sodium API Compatibility
During the implementation of reactive distance calculations, I initially used:
-------------------------------------------------
historyCell.updates()
-------------------------------------------------

However, compilation produced the error:
-------------------------------------------------
updates() is not public in Cell;
cannot be accessed from outside package
-------------------------------------------------

ChatGPT then suggested:
-------------------------------------------------
historyCell.value()
-------------------------------------------------
This also failed because the available method required a `Transaction` argument.
After reviewing these errors, the implementation was revised to use `lift()` and `listen()`.
This was a useful example of why AI-generated code needed to be checked against the installed libraries rather than assumed to be correct.


## 6. Testing and Verification
ChatGPT also assisted with developing JUnit tests and identifying edge cases that should be verified.
The tests covered distance calculations, altitude conversion, coordinate filtering, five-minute history management, and event-clearing behaviour.

### Example 6: Testing Five-Minute Distance Calculations
ChatGPT helped develop tests for the sliding distance window, including checking that movement outside the five-minute interval was no longer counted.

One example from `GpsDistanceWindow_Test.java` is:
-------------------------------------------------
@Test
public void testExactFiveMinuteBoundary() {

    GpsDistanceWindow window = new GpsDistanceWindow();

    GpsEvent first =
        new GpsEvent("Tracker0", 40.0, 116.0, 100.0);

    GpsEvent second =
        new GpsEvent("Tracker0", 40.001, 116.0, 100.0);

    window.addEvent(first, 0);
    window.addEvent(second, 60000);

    double expected =
        GpsDistance.calculateDistance(first, second);

    assertEquals(
        expected,
        window.getDistance(300000),
        0.0001
    );

    assertEquals(
        0.0,
        window.getDistance(300001),
        0.0001
    );
}
-------------------------------------------------
This test helped verify the behaviour at the exact five-minute boundary rather than only testing ordinary movement.


## 7. Reflection
Using ChatGPT was particularly helpful when I encountered concepts or errors that I did not fully understand.

The explanations of Sodium operations helped me understand how the different parts of the reactive processing pipeline fit together. Debugging discussions also helped me identify problems with Java compilation, external libraries, and the implementation of GPS history and distance calculations.

Not every suggestion worked immediately. Some required changes after compilation errors or unexpected behaviour during testing.

Overall, I used ChatGPT as a learning and debugging resource, alongside assistance with minor code implementation and testing. The examples above describe its main contributions and the verification I performed during development.