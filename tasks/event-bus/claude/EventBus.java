import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A lightweight, type-safe, thread-safe event bus supporting prioritized listeners,
 * error handling, and extensibility via error handlers and interceptors.
 */
public class EventBus {

    /**
     * Internal holder for a listener with its priority.
     */
    private record ListenerEntry<T extends Event>(
            int priority,
            EventListener<T> listener
    ) implements Comparable<ListenerEntry<T>> {
        @Override
        public int compareTo(ListenerEntry<T> other) {
            // Higher priority first
            return Integer.compare(other.priority, this.priority);
        }
    }

    private final ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArrayList<ListenerEntry<?>>> listeners =
            new ConcurrentHashMap<>();

    private volatile Consumer<Exception> errorHandler = e ->
            System.err.println("[EventBus] Listener threw exception: " + e.getMessage());

    /**
     * Sets a custom error handler invoked when a listener throws an exception.
     */
    public void setErrorHandler(Consumer<Exception> errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Subscribe a listener for a specific event type with default priority (0).
     */
    public <T extends Event> Subscription subscribe(Class<T> eventType, EventListener<T> listener) {
        return subscribe(eventType, listener, 0);
    }

    /**
     * Subscribe a listener for a specific event type with a given priority.
     * Higher priority listeners are invoked first.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> Subscription subscribe(Class<T> eventType, EventListener<T> listener, int priority) {
        var entry = new ListenerEntry<>(priority, listener);
        var list = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());

        // Insert in sorted position (higher priority first)
        synchronized (list) {
            int insertIdx = 0;
            for (int i = 0; i < list.size(); i++) {
                if (((ListenerEntry<T>) list.get(i)).priority() >= priority) {
                    insertIdx = i + 1;
                } else {
                    break;
                }
            }
            list.add(insertIdx, entry);
        }

        return new Subscription(() -> {
            synchronized (list) {
                list.remove(entry);
            }
        });
    }

    /**
     * Publish an event to all listeners subscribed to its exact type and supertypes.
     * Listeners are invoked synchronously in priority order.
     * Exceptions in one listener do not prevent others from executing.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        // Walk the class hierarchy to support listeners on parent event types
        Class<?> clazz = event.getClass();
        while (Event.class.isAssignableFrom(clazz)) {
            var list = listeners.get(clazz);
            if (list != null) {
                for (var entry : list) {
                    try {
                        ((ListenerEntry<T>) entry).listener().onEvent(event);
                    } catch (Exception e) {
                        try {
                            errorHandler.accept(e);
                        } catch (Exception ignored) {
                            // Don't let error handler failures propagate
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Remove all listeners for a specific event type.
     */
    public void clearListeners(Class<? extends Event> eventType) {
        listeners.remove(eventType);
    }

    /**
     * Remove all listeners from the bus.
     */
    public void clearAll() {
        listeners.clear();
    }

    /**
     * Returns the number of listeners registered for a given event type.
     */
    public int listenerCount(Class<? extends Event> eventType) {
        var list = listeners.get(eventType);
        return list == null ? 0 : list.size();
    }
}