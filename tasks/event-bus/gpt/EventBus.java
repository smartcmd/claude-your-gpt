import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class EventBus {
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<RegisteredListener<?>>> listenersByType =
            new ConcurrentHashMap<>();
    private final AtomicLong registrationSequence = new AtomicLong();
    private final ErrorHandler errorHandler;

    public EventBus() {
        this(ErrorHandler.loggingToStdErr());
    }

    public EventBus(ErrorHandler errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public <T> Subscription subscribe(Class<T> eventType, EventListener<? super T> listener) {
        return subscribe(eventType, 0, listener);
    }

    public <T> Subscription subscribe(Class<T> eventType, int priority, EventListener<? super T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");

        var registeredListener = new RegisteredListener<>(
                eventType,
                listener,
                priority,
                registrationSequence.getAndIncrement()
        );

        listenersByType
                .computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>())
                .add(registeredListener);

        return () -> unsubscribe(registeredListener);
    }

    public void publish(Object event) {
        Objects.requireNonNull(event, "event");

        for (RegisteredListener<?> listener : matchingListeners(event)) {
            try {
                listener.invoke(event);
            } catch (Exception exception) {
                errorHandler.onError(event, listener, exception);
            }
        }
    }

    public int listenerCount() {
        int count = 0;
        for (CopyOnWriteArrayList<RegisteredListener<?>> listeners : listenersByType.values()) {
            count += listeners.size();
        }
        return count;
    }

    private void unsubscribe(RegisteredListener<?> listener) {
        CopyOnWriteArrayList<RegisteredListener<?>> listeners = listenersByType.get(listener.eventType());
        if (listeners == null) {
            return;
        }

        listeners.remove(listener);
        if (listeners.isEmpty()) {
            listenersByType.remove(listener.eventType(), listeners);
        }
    }

    private List<RegisteredListener<?>> matchingListeners(Object event) {
        List<RegisteredListener<?>> matches = new ArrayList<>();
        Class<?> eventClass = event.getClass();

        for (var entry : listenersByType.entrySet()) {
            if (!entry.getKey().isAssignableFrom(eventClass)) {
                continue;
            }
            matches.addAll(entry.getValue());
        }

        matches.sort(null);
        return matches;
    }

}
