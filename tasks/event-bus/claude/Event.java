/**
 * Base class for all events in the event bus system.
 * Subclass this to define specific event types.
 */
public abstract class Event {

    private boolean cancelled;
    private final long timestamp;

    protected Event() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}