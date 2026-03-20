record RegisteredListener<T>(
        Class<T> eventType,
        EventListener<? super T> listener,
        int priority,
        long sequence
) implements ListenerMetadata, Comparable<RegisteredListener<?>> {
    void invoke(Object event) throws Exception {
        listener.onEvent(eventType.cast(event));
    }

    @Override
    public int compareTo(RegisteredListener<?> other) {
        int byPriority = Integer.compare(other.priority(), priority);
        if (byPriority != 0) {
            return byPriority;
        }
        return Long.compare(sequence, other.sequence());
    }
}
