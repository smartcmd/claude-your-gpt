/**
 * Functional interface for event listeners.
 *
 * @param <T> the event type this listener handles
 */
@FunctionalInterface
public interface EventListener<T extends Event> {
    void onEvent(T event);
}