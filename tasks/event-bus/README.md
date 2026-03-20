# Event Bus

Implement a lightweight event bus system that allows components to communicate via events. Your implementation must support:

1. Event publishing and subscription

- Allow registering listeners/subscribers for specific event types
- Allow unregistering listeners
- Support publishing events to all relevant listeners

2. Type-safe event handling

- Listeners should receive only the event types they are subscribed to
- Avoid unsafe casting where possible

3. Synchronous event dispatch

- Events should be delivered to listeners synchronously by default
- Listeners are invoked in a predictable order

4. Listener ordering (optional but preferred)

- Allow assigning priority to listeners
- Higher priority listeners should be invoked earlier

5. Extensibility

- The system should be designed to allow future extensions (e.g., asynchronous dispatch, filtering, interceptors)

6. Error handling

- Exceptions thrown by one listener should not prevent other listeners from executing
- Provide a reasonable strategy for handling or reporting listener errors

7. Thread-safety

- The event bus should behave correctly under concurrent access (e.g., registering listeners while publishing events)

## Requirements

- Using Java 21
- Place the source files in the directory directly. Do not using any build tools like Gradle or Maven
- PLace the test cases in the main method