import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public final class EventBusMain {
    public static void main(String[] args) throws InterruptedException {
        testPublishAndUnregister();
        testPriorityAndRegistrationOrder();
        testHierarchicalDispatch();
        testErrorIsolation();
        testConcurrentRegisterAndPublish();
        System.out.println("All EventBus tests passed.");
    }

    private static void testPublishAndUnregister() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();

        Subscription subscription = bus.subscribe(MessageEvent.class, event -> received.add(event.message()));
        bus.publish(new MessageEvent("first"));
        subscription.unsubscribe();
        bus.publish(new MessageEvent("second"));

        assertEquals(List.of("first"), received, "Listeners should receive subscribed events only while registered");
        assertEquals(0, bus.listenerCount(), "Unsubscribed listeners should be removed");
    }

    private static void testPriorityAndRegistrationOrder() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(MessageEvent.class, 0, event -> received.add("normal-1:" + event.message()));
        bus.subscribe(MessageEvent.class, 10, event -> received.add("high:" + event.message()));
        bus.subscribe(MessageEvent.class, 0, event -> received.add("normal-2:" + event.message()));

        bus.publish(new MessageEvent("ordered"));

        assertEquals(
                List.of("high:ordered", "normal-1:ordered", "normal-2:ordered"),
                received,
                "Higher priority listeners should run first, then earlier registrations"
        );
    }

    private static void testHierarchicalDispatch() {
        EventBus bus = new EventBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(NamedEvent.class, 5, event -> received.add("base:" + event.name()));
        bus.subscribe(SpecialEvent.class, 0, event -> received.add("special:" + event.name()));

        bus.publish(new SpecialEvent("child"));

        assertEquals(
                List.of("base:child", "special:child"),
                received,
                "Listeners registered for supertypes should receive subtype events"
        );
    }

    private static void testErrorIsolation() {
        List<String> errors = new ArrayList<>();
        EventBus bus = new EventBus((event, listener, exception) ->
                errors.add(event + "|" + listener.eventType().getSimpleName() + "|" + exception.getMessage()));
        List<String> received = new ArrayList<>();

        bus.subscribe(MessageEvent.class, 10, event -> {
            throw new IllegalStateException("boom");
        });
        bus.subscribe(MessageEvent.class, 0, event -> received.add(event.message()));

        bus.publish(new MessageEvent("safe"));

        assertEquals(List.of("safe"), received, "A failing listener must not stop later listeners");
        assertEquals(
                List.of("MessageEvent[message=safe]|MessageEvent|boom"),
                errors,
                "Errors should be reported through the configured handler"
        );
    }

    private static void testConcurrentRegisterAndPublish() throws InterruptedException {
        EventBus bus = new EventBus();
        List<Integer> received = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread publisher = new Thread(() -> {
            ready.countDown();
            await(start);
            for (int i = 0; i < 500; i++) {
                bus.publish(new NumberEvent(i));
            }
            done.countDown();
        });

        Thread subscriber = new Thread(() -> {
            ready.countDown();
            await(start);
            for (int i = 0; i < 50; i++) {
                bus.subscribe(NumberEvent.class, event -> received.add(event.value()));
            }
            done.countDown();
        });

        publisher.start();
        subscriber.start();
        ready.await();
        start.countDown();
        done.await();

        bus.publish(new NumberEvent(999));

        if (received.stream().noneMatch(value -> value == 999)) {
            throw new AssertionError("Concurrent subscriptions should remain usable after publishing");
        }
        if (received.isEmpty()) {
            throw new AssertionError("Concurrent publishing should deliver at least some events");
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Unexpected interruption", exception);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + System.lineSeparator()
                    + "Expected: " + expected + System.lineSeparator()
                    + "Actual:   " + actual);
        }
    }

    private record SpecialEvent(String name) implements NamedEvent {
    }

    private record MessageEvent(String message) {
    }

    private record NumberEvent(int value) {
    }

    private interface NamedEvent {
        String name();
    }
}
