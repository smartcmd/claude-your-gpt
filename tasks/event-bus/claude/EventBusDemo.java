import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class EventBusDemo {

    // --- Sample event types ---

    static class MessageEvent extends Event {
        private final String message;
        MessageEvent(String message) { this.message = message; }
        String getMessage() { return message; }
    }

    static class UserEvent extends Event {
        private final String username;
        UserEvent(String username) { this.username = username; }
        String getUsername() { return username; }
    }

    static class LoginEvent extends UserEvent {
        LoginEvent(String username) { super(username); }
    }

    static class LogoutEvent extends UserEvent {
        LogoutEvent(String username) { super(username); }
    }

    // --- Test helpers ---

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + name);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
        }
    }

    // --- Tests ---

    public static void main(String[] args) throws Exception {
        testBasicPubSub();
        testTypeSafety();
        testUnsubscribe();
        testPriorityOrdering();
        testErrorHandling();
        testHierarchicalDispatch();
        testCancelledEvent();
        testThreadSafety();
        testClearListeners();

        System.out.println("\n=============================");
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void testBasicPubSub() {
        System.out.println("\n[Basic Publish/Subscribe]");
        var bus = new EventBus();
        var received = new ArrayList<String>();

        bus.subscribe(MessageEvent.class, e -> received.add(e.getMessage()));
        bus.publish(new MessageEvent("hello"));
        bus.publish(new MessageEvent("world"));

        check("received 2 messages", received.size() == 2);
        check("first message correct", received.get(0).equals("hello"));
        check("second message correct", received.get(1).equals("world"));
    }

    static void testTypeSafety() {
        System.out.println("\n[Type Safety]");
        var bus = new EventBus();
        var messages = new ArrayList<String>();
        var users = new ArrayList<String>();

        bus.subscribe(MessageEvent.class, e -> messages.add(e.getMessage()));
        bus.subscribe(UserEvent.class, e -> users.add(e.getUsername()));

        bus.publish(new MessageEvent("msg1"));
        bus.publish(new LoginEvent("alice"));

        check("message listener got 1 event", messages.size() == 1);
        check("user listener got 1 event (from LoginEvent)", users.size() == 1);
        check("user listener got correct username", users.get(0).equals("alice"));
    }

    static void testUnsubscribe() {
        System.out.println("\n[Unsubscribe]");
        var bus = new EventBus();
        var count = new AtomicInteger(0);

        var sub = bus.subscribe(MessageEvent.class, e -> count.incrementAndGet());
        bus.publish(new MessageEvent("a"));
        check("received before unsubscribe", count.get() == 1);

        sub.unsubscribe();
        bus.publish(new MessageEvent("b"));
        check("not received after unsubscribe", count.get() == 1);
        check("subscription inactive", !sub.isActive());
        check("listener count is 0", bus.listenerCount(MessageEvent.class) == 0);
    }

    static void testPriorityOrdering() {
        System.out.println("\n[Priority Ordering]");
        var bus = new EventBus();
        var order = new ArrayList<String>();

        bus.subscribe(MessageEvent.class, e -> order.add("low"), -10);
        bus.subscribe(MessageEvent.class, e -> order.add("normal"), 0);
        bus.subscribe(MessageEvent.class, e -> order.add("high"), 10);

        bus.publish(new MessageEvent("test"));

        check("3 listeners invoked", order.size() == 3);
        check("high priority first", order.get(0).equals("high"));
        check("normal priority second", order.get(1).equals("normal"));
        check("low priority last", order.get(2).equals("low"));
    }

    static void testErrorHandling() {
        System.out.println("\n[Error Handling]");
        var bus = new EventBus();
        var results = new ArrayList<String>();
        var errors = new ArrayList<Exception>();

        bus.setErrorHandler(errors::add);

        bus.subscribe(MessageEvent.class, e -> results.add("before"), 10);
        bus.subscribe(MessageEvent.class, e -> { throw new RuntimeException("boom"); }, 5);
        bus.subscribe(MessageEvent.class, e -> results.add("after"), 0);

        bus.publish(new MessageEvent("test"));

        check("first listener executed", results.contains("before"));
        check("third listener executed despite error in second", results.contains("after"));
        check("error was captured", errors.size() == 1);
        check("error message correct", errors.get(0).getMessage().equals("boom"));
    }

    static void testHierarchicalDispatch() {
        System.out.println("\n[Hierarchical Dispatch]");
        var bus = new EventBus();
        var userEvents = new ArrayList<String>();
        var loginEvents = new ArrayList<String>();

        bus.subscribe(UserEvent.class, e -> userEvents.add(e.getUsername()));
        bus.subscribe(LoginEvent.class, e -> loginEvents.add(e.getUsername()));

        bus.publish(new LoginEvent("bob"));

        check("LoginEvent listener received", loginEvents.size() == 1);
        check("UserEvent listener also received (hierarchy)", userEvents.size() == 1);

        bus.publish(new LogoutEvent("carol"));
        check("UserEvent listener received LogoutEvent", userEvents.size() == 2);
        check("LoginEvent listener did not receive LogoutEvent", loginEvents.size() == 1);
    }

    static void testCancelledEvent() {
        System.out.println("\n[Cancelled Event]");
        var bus = new EventBus();
        var results = new ArrayList<String>();

        bus.subscribe(MessageEvent.class, e -> {
            results.add("first");
            e.setCancelled(true);
        }, 10);
        bus.subscribe(MessageEvent.class, e -> {
            if (!e.isCancelled()) results.add("second");
        }, 0);

        bus.publish(new MessageEvent("test"));

        check("first listener ran", results.contains("first"));
        check("second listener respected cancellation", !results.contains("second"));
    }

    static void testThreadSafety() throws Exception {
        System.out.println("\n[Thread Safety]");
        var bus = new EventBus();
        var counter = new AtomicInteger(0);
        int numThreads = 10;
        int eventsPerThread = 100;

        bus.subscribe(MessageEvent.class, e -> counter.incrementAndGet());

        var latch = new CountDownLatch(numThreads);
        var errors = Collections.synchronizedList(new ArrayList<Exception>());

        for (int t = 0; t < numThreads; t++) {
            int threadId = t;
            Thread.ofVirtual().start(() -> {
                try {
                    // Some threads publish, some subscribe/unsubscribe concurrently
                    if (threadId % 2 == 0) {
                        for (int i = 0; i < eventsPerThread; i++) {
                            bus.publish(new MessageEvent("t" + threadId + "-" + i));
                        }
                    } else {
                        for (int i = 0; i < eventsPerThread; i++) {
                            var sub = bus.subscribe(MessageEvent.class, e -> {});
                            sub.unsubscribe();
                        }
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        check("no concurrent exceptions", errors.isEmpty());
        check("events were delivered (counter > 0)", counter.get() > 0);
    }

    static void testClearListeners() {
        System.out.println("\n[Clear Listeners]");
        var bus = new EventBus();
        var count = new AtomicInteger(0);

        bus.subscribe(MessageEvent.class, e -> count.incrementAndGet());
        bus.subscribe(MessageEvent.class, e -> count.incrementAndGet());

        check("2 listeners registered", bus.listenerCount(MessageEvent.class) == 2);

        bus.clearListeners(MessageEvent.class);
        bus.publish(new MessageEvent("test"));

        check("no events after clear", count.get() == 0);
        check("listener count is 0", bus.listenerCount(MessageEvent.class) == 0);
    }
}