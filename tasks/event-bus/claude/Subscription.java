/**
 * Represents a registered listener subscription.
 * Can be used to unsubscribe.
 */
public final class Subscription {

    private final Runnable unsubscribeAction;
    private volatile boolean active = true;

    Subscription(Runnable unsubscribeAction) {
        this.unsubscribeAction = unsubscribeAction;
    }

    public void unsubscribe() {
        if (active) {
            active = false;
            unsubscribeAction.run();
        }
    }

    public boolean isActive() {
        return active;
    }
}