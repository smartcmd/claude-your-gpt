# GPT vs Claude: Cross-Task Code Style Analysis

Three tasks — **Event Bus**, **Expression Parser**, **Password Validator** — were implemented independently by GPT and Claude under the same requirements (Java 21, single-file-runnable, tests in `main`). This document distills the recurring GPT style problems observed across all three.

---

## 1. Over-Abstraction Without Purpose

**Severity: High | Observed in: All 3 tasks**

GPT consistently introduces more types, interfaces, and files than the problem demands:

| Task               | GPT                                                                                           | Claude                                                  | Delta                                            |
|--------------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------|--------------------------------------------------|
| Event Bus          | 7 files (`ListenerMetadata` interface, `RegisteredListener` record, `ErrorHandler` interface) | 6 files (internal `ListenerEntry` private record)       | +1 public type that is purely an internal detail |
| Expression Parser  | Plain `Node` interface + 5 separate record implementations with `evaluate()`                  | `sealed interface Expr` + centralized `evaluate()`      | 5 scattered eval sites vs 1                      |
| Password Validator | `PasswordValidationConfig` with full Builder (88 lines)                                       | `ValidationRule` functional interface + `Rules` factory | Builder ceremony for a closed set of booleans    |

The pattern: GPT reaches for **enterprise-weight scaffolding** (interfaces-per-concept, Builder patterns, metadata abstractions) regardless of the problem scale. Claude keeps implementation details private and chooses the lightest abstraction that works.

**Root cause:** GPT defaults to "production Java" boilerplate patterns even when the task scope doesn't warrant them. More files and more interfaces do not equal better design — they equal more surface area to maintain with no added extensibility.

---

## 2. Ignoring Modern Java Features

**Severity: High | Observed in: All 3 tasks**

The tasks specify Java 21, yet GPT writes code that would compile unchanged on Java 11:

| Feature                   | GPT Usage                | Claude Usage                                                                  |
|---------------------------|--------------------------|-------------------------------------------------------------------------------|
| `sealed interface`        | Never                    | Expression Parser AST — compiler-enforced exhaustiveness                      |
| Pattern matching `switch` | Never                    | Expression Parser evaluator — all cases in one 25-line block                  |
| `record` as result type   | Partial (AST nodes only) | Password Validator `ValidationResult` — 5 lines vs GPT's 30-line manual class |
| Virtual threads           | Never                    | Event Bus thread-safety test with `Thread.ofVirtual()`                        |

**Specific example — Expression Parser:**
Claude's `sealed interface Expr permits NumberExpr, VarExpr, BinaryExpr, UnaryExpr, CallExpr` makes the compiler enforce that every `switch` over `Expr` handles all variants. GPT's open `interface Node` with per-class `evaluate()` provides no such safety net — adding a new node type silently compiles without handling it elsewhere.

**Specific example — Password Validator:**
GPT writes a 30-line immutable class (`PasswordValidationResult`) with private constructor, static factories, getters, and `List.copyOf()`. Claude achieves identical semantics with `record ValidationResult(boolean passed, List<String> failures) {}` — 5 lines, with `equals`/`hashCode`/`toString` for free.

**Root cause:** GPT appears to be trained on pre-Java-17 codebases and does not adapt to the specified language level. This results in verbose, manually-written code that modern Java eliminates entirely.

---

## 3. Defensive Coding to a Fault

**Severity: Medium | Observed in: All 3 tasks**

GPT peppers every public surface with `Objects.requireNonNull()`, null-checks, and precondition validations — even where callers are internal and controlled:

- **Event Bus:** `Objects.requireNonNull` on every parameter of `subscribe()` and `publish()`
- **Expression Parser:** `input == null ? "" : input`, `name.isBlank()` checks, `List.copyOf()` on argument lists
- **Password Validator:** Null-check on password input, config null-check, special-character null/empty check in Builder, negative-length check — 5+ defensive guards for a single-file demo

Claude validates at genuine system boundaries and trusts internal code. The result is less noise and faster reading.

**Why it matters:** Defensive checks have a cost — not in runtime, but in readability. When every line is "guarded," the reader cannot distinguish true boundaries (user input, external API calls) from internal plumbing. The signal-to-noise ratio drops. For tasks with no external callers, this is pure ceremony.

---

## 4. Algorithmically Suboptimal Choices

**Severity: Medium | Observed in: Event Bus, Expression Parser**

GPT picks straightforward-but-slow approaches where a more considered choice costs the same code complexity:

**Event Bus — Priority dispatch:**
- GPT: Collects all matching listeners into a new `ArrayList`, then calls `matches.sort()` on **every** `publish()` — O(n log n) per dispatch.
- Claude: Inserts listeners in sorted order at subscribe time — O(n) at subscribe, O(1) iteration at publish. Since event buses are read-heavy, Claude's choice is structurally correct.

**Event Bus — Hierarchical dispatch:**
- GPT: Iterates over **all** registered event types and checks `isAssignableFrom()` for each — O(total registered types).
- Claude: Walks up the class hierarchy with `getSuperclass()` — O(depth of inheritance), typically 2–3.

**Expression Parser — Unary plus:**
- GPT: Creates `new UnaryNode('+', ...)` — allocates a node that evaluates to its operand unchanged.
- Claude: Simply recurses without wrapping — no allocation, no unnecessary AST depth.

---

## 5. Weaker Extensibility Model

**Severity: Medium-High | Observed in: Event Bus, Password Validator**

GPT's designs appear configurable but are actually closed:

**Password Validator:**
GPT's `PasswordValidationConfig.Builder` exposes `requireUppercase(true)`, `requireDigit(true)`, etc. — boolean toggles for a **fixed** set of rules. Adding "no repeating characters" means editing the `PasswordValidator` class itself. Claude's `ValidationRule` functional interface means adding a rule is a one-liner lambda with zero existing code changes. The task asked for "configurable validation rules" — GPT delivered configuration toggles, not composable rules.

**Event Bus:**
GPT's `publish(Object event)` accepts any object — no type constraint. Claude's `EventListener<T extends Event>` enforces at compile time that listeners can only subscribe to actual events. GPT's `EventListener<T>` is unbounded, so `bus.subscribe(String.class, ...)` compiles fine but is semantically wrong.

---

## 6. Verbose and Redundant Naming

**Severity: Low | Observed in: Password Validator**

GPT prefixes every class with the domain word: `PasswordValidationConfig`, `PasswordValidationResult`, `PasswordValidatorMain`. These classes already exist in a password-validator context — the prefix adds no information. Claude uses concise names: `ValidationRule`, `ValidationResult`, `Rules`.

This extends to method names and variable names across tasks. GPT tends toward longer names that repeat context already clear from the enclosing class or package.

---

## 7. Test Coverage Gaps

**Severity: Medium | Observed in: All 3 tasks**

| Task | GPT Tests | Claude Tests | GPT Missing |
|------|-----------|--------------|-------------|
| Event Bus | 5 | 9 | Event cancellation, clear/reset, subscription state tracking |
| Expression Parser | 9 | 28 | Nested parentheses, no-space expressions, tab handling, chained operations |
| Password Validator | 5 | 9 | Edge cases with custom rule composition |

Beyond count, Claude's tests are better **organized** (section headers, pass/fail counters, non-zero exit on failure) and better **scoped** (testing features that actually exist in the implementation — e.g., event cancellation, which GPT doesn't implement).

GPT's tests use `AssertionError` throws with no per-assertion reporting — a single failure crashes the entire suite with no indication of which tests passed.

---

## 8. Thread-Safety Bugs

**Severity: High (but narrow) | Observed in: Event Bus**

GPT's `unsubscribe()` contains a race condition:

```java
listeners.remove(listener);
if (listeners.isEmpty()) {
    listenersByType.remove(listener.eventType(), listeners);
}
```

The `remove` → `isEmpty()` → conditional `remove` sequence is **not atomic**. Between `listeners.isEmpty()` returning `true` and `listenersByType.remove()` executing, another thread could add a listener to the same list. The newly-added listener's backing list would be silently removed from the map — a data-loss bug. Claude avoids this pattern entirely.

---

## Summary Table

| Problem                    | Severity    | Event Bus | Expr Parser | Pwd Validator |
|----------------------------|-------------|-----------|-------------|---------------|
| Over-abstraction           | High        | Yes       | Yes         | Yes           |
| Ignoring Java 21 features  | High        | Yes       | Yes         | Yes           |
| Excessive defensive coding | Medium      | Yes       | Yes         | Yes           |
| Algorithmic inefficiency   | Medium      | Yes       | Yes         | —             |
| Closed extensibility model | Medium-High | Yes       | —           | Yes           |
| Redundant naming           | Low         | —         | —           | Yes           |
| Weaker test coverage       | Medium      | Yes       | Yes         | Yes           |
| Thread-safety bugs         | High        | Yes       | —           | —             |

---

## Conclusion

GPT's code across all three tasks exhibits a consistent pattern: **ceremony over substance**. It reaches for heavyweight patterns (Builder, metadata interfaces, manual immutable classes) and defensive boilerplate that inflate code volume without adding capability. It ignores the Java 21 features (`sealed`, pattern matching, records-as-data, virtual threads) that would make the code both shorter and safer. And it makes algorithmically poor choices that a more careful implementation avoids.

Claude's implementations are more **cohesive** (fewer files, implementation details kept private), more **idiomatic** (using the language features the task specifies), more **extensible** (open rule systems, type-safe hierarchies), and more **thoroughly tested** (2–3x test coverage). The core difference is not capability — GPT's code works — but **taste**: knowing when an abstraction earns its weight and when it's just noise.