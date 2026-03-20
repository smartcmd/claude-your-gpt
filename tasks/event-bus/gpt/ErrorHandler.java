@FunctionalInterface
public interface ErrorHandler {
    void onError(Object event, ListenerMetadata listenerMetadata, Exception exception);

    static ErrorHandler loggingToStdErr() {
        return (event, listenerMetadata, exception) -> {
            System.err.printf(
                    "Listener failure for event %s handled by %s (priority=%d)%n",
                    event,
                    listenerMetadata.eventType().getSimpleName(),
                    listenerMetadata.priority()
            );
            exception.printStackTrace(System.err);
        };
    }
}
