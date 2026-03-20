public interface ListenerMetadata {
    Class<?> eventType();

    int priority();

    long sequence();
}
