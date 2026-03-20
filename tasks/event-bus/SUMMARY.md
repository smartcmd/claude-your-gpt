# Event Bus: Claude vs GPT Code Comparison

## Overview

Both implementations deliver a functional event bus with publish/subscribe, priority ordering, error handling, hierarchical dispatch, and thread safety. However, they diverge significantly in design philosophy, API ergonomics, and implementation quality.

| Aspect                | Claude                                                          | GPT                                             |
|-----------------------|-----------------------------------------------------------------|-------------------------------------------------|
| Files                 | 6                                                               | 7                                               |
| Event base type       | Abstract `Event` class                                          | None (`Object`)                                 |
| Subscription          | Stateful class                                                  | Functional interface                            |
| Priority ordering     | Sorted insertion (O(n) insert)                                  | Sort-on-every-publish (O(n log n) per dispatch) |
| Hierarchical dispatch | Walks superclass chain                                          | Iterates all registered types                   |
| Test cases            | 9                                                               | 5                                               |
| Extra features        | Cancellation, clearListeners, clearAll, per-type listener count | Registration-sequence tiebreaker                |

---

## Where GPT Falls Short Stylistically

### 1. Over-Abstraction and File Proliferation

GPT splits its code into 7 files, introducing `ListenerMetadata` (an interface with one implementation), `RegisteredListener` (a top-level record that is purely an internal detail), and `ErrorHandler` (a separate functional interface). Claude keeps its equivalent `ListenerEntry` as a **private nested record** inside `EventBus`, correctly treating it as an implementation detail rather than a public API surface.

The `ListenerMetadata` interface is a textbook example of premature abstraction — it exists solely to be implemented by `RegisteredListener`, adding indirection without actual extensibility. This "interface-per-concept" tendency is a hallmark of over-engineered GPT output: more files, more types, but no additional capability.

### 2. No Event Base Type — Loss of Type Safety

GPT's `publish(Object event)` accepts literally anything. There is no compile-time constraint on what constitutes an "event." Claude defines an abstract `Event` class that serves as a bounded type, meaning `EventListener<T extends Event>` enforces at compile time that listeners can only subscribe to actual events. GPT's unbounded `EventListener<T>` provides no such guarantee.

This also means GPT misses the opportunity to add shared event behavior. Claude's `Event` includes `timestamp` and `cancelled` fields — useful cross-cutting concerns — for free. GPT would need to duplicate this in every event type or bolt it on later.

### 3. Subscription Lacks State Tracking

GPT's `Subscription` is a bare `@FunctionalInterface` with a single `unsubscribe()` method. Claude's `Subscription` is a proper class that:
- Tracks `isActive()` state via a `volatile boolean`
- Guards against double-unsubscribe (idempotent)
- Provides introspection (`isActive()`)

GPT's lambda-based subscription (`return () -> unsubscribe(registeredListener)`) is concise but fragile — calling `unsubscribe()` twice triggers redundant list operations. This is a case where GPT optimized for brevity over correctness.

### 4. Inefficient Dispatch Strategy

**Priority ordering:** Claude inserts listeners in sorted order at subscription time. Since event buses are read-heavy (many publishes, few subscribes), this amortizes the cost correctly — O(n) at subscribe, O(1) iteration at publish. GPT collects all matching listeners into a new `ArrayList`, then calls `matches.sort(null)` on every single `publish()` call — O(n log n) per dispatch. This is algorithmically backwards for a typical event bus workload.

**Hierarchical dispatch:** Claude walks up the class hierarchy with `getSuperclass()`, which is O(depth of inheritance). GPT iterates over **all registered event types** and checks `isAssignableFrom()` for each one — O(number of registered types). In a system with many event types, GPT's approach degrades linearly with the total number of subscriptions, not just the relevant ones.

### 5. Weaker API Surface

GPT's `EventBus` exposes only:
- `subscribe`, `publish`, `listenerCount()` (total count, not per-type)

Claude's `EventBus` additionally provides:
- `clearListeners(Class)` — remove all listeners for a specific event type
- `clearAll()` — reset the bus entirely
- `listenerCount(Class)` — count per event type (far more useful for diagnostics)
- `setErrorHandler()` — swap error handling strategy at runtime

GPT's `listenerCount()` returns the global total, which is nearly useless for debugging. Claude's per-type variant tells you exactly what's subscribed where.

### 6. Test Coverage and Quality

Claude provides 9 well-structured tests with individual PASS/FAIL reporting:
- Basic pub/sub, type safety, unsubscribe, priority ordering, error handling, hierarchical dispatch, **event cancellation**, thread safety (with virtual threads), and clear listeners.

GPT provides 5 tests using `AssertionError` throws (note: no per-assertion reporting, just pass/crash). Missing coverage includes:
- No cancellation test (feature doesn't exist)
- No clear/reset test (feature doesn't exist)
- No subscription state test (`isActive()` doesn't exist)

Claude's thread safety test also uses **virtual threads** (`Thread.ofVirtual()`), demonstrating idiomatic Java 21 usage, while GPT uses platform threads — functional but not leveraging the specified Java 21 requirement.

### 7. Subtle Thread-Safety Issue in GPT's Unsubscribe

GPT's `unsubscribe` method:
```java
listeners.remove(listener);
if (listeners.isEmpty()) {
    listenersByType.remove(listener.eventType(), listeners);
}
```

The `remove` + `isEmpty` check + conditional `remove` sequence is **not atomic**. Between `listeners.isEmpty()` returning `true` and `listenersByType.remove()` executing, another thread could add a new listener to the same list. This would cause the newly-added listener's backing list to be silently removed from the map. Claude avoids this pattern entirely.

---

## What GPT Does Well

- **Registration sequence tiebreaker**: GPT's `RegisteredListener` uses an `AtomicLong` sequence number to guarantee stable ordering among same-priority listeners. Claude's sorted insertion preserves insertion order implicitly via `CopyOnWriteArrayList`, but GPT's approach is more explicit.
- **Null-checking**: GPT uses `Objects.requireNonNull` on public API parameters — a good defensive practice that Claude omits.
- **Records as events in tests**: GPT's test events are records (`record MessageEvent(String message)`), which is more concise and idiomatic Java 16+ than Claude's inner classes with manual constructors/getters.
- **Richer error context**: GPT's `ErrorHandler` receives the event, listener metadata, and exception — more diagnostic information than Claude's `Consumer<Exception>`.

---

## Conclusion

Claude's implementation is more **cohesive, efficient, and feature-complete**. It makes pragmatic design choices: keeping implementation details private, choosing the right data structure for the workload, providing a richer API surface, and writing thorough tests. GPT's implementation suffers from a common pattern in LLM-generated code: **over-decomposition without purpose** — more files, more interfaces, more indirection, but less actual functionality and worse runtime characteristics. The abstraction boundaries in GPT's code exist for their own sake rather than to solve real extensibility needs.