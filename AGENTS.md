# Code Style Requirements

You must follow these style rules strictly:

## DO NOT OVER-ABSTRACT

- Keep implementation details private. If a type is only used internally by one module/class, nest it or keep it unexported — do not make it a separate public file.
- Do not create an abstraction (interface, trait, protocol, abstract class) that has exactly one implementation. That is not abstraction, it is indirection.
- Do not use heavyweight construction patterns (Builder, fluent config objects) unless the object has 5+ required fields or genuinely complex construction. Prefer simple constructors, factory functions, or struct/dict literals.
- Fewer files with cohesive internals are better than many files with thin wrappers.
- Prefer composable, open designs (callbacks, lambdas, higher-order functions, strategy objects) over closed configuration (boolean toggles, enum switches for a fixed set of options). When the task asks for "configurable rules," deliver composable rules — not a fixed list of on/off flags.

## DO NOT OVER-DEFEND

- Only add defensive checks (null/nil/None checks, type guards, boundary validation) at true system boundaries — public API entry points that accept external, untrusted input.
- Do not add defensive checks in internal/private functions, constructors called only by your own code, or test helpers.
- Do not add defensive copies unless the data is genuinely shared across trust boundaries.
- Omitting a defensive check is not a bug — it is a deliberate signal that the caller is trusted.

## USE MODERN LANGUAGE FEATURES

- Write idiomatic code for the language version specified by the project. Do not write code that targets an older version out of habit.
- Prefer language-level constructs that reduce boilerplate: pattern matching, destructuring, algebraic data types (sealed types, tagged unions, enums with data), data classes/records/structs, and built-in concurrency primitives.
- If the language provides exhaustiveness checking (e.g., sealed types + switch, match expressions, tagged unions), use it. Compiler-enforced completeness is better than a default/else branch that hides missing cases.
- Do not manually write what the language generates for free (toString, equality, hash, serialization).

## CHOOSE GOOD ALGORITHMS

- Think about the read/write ratio of your data structure. For read-heavy paths, pay the cost at write time (sorted insertion, precomputed indexes). Do not sort or scan on every read.
- Prefer the narrower traversal. Walk a known-depth hierarchy rather than scanning all registered entries. Filter early rather than collecting everything then filtering.
- Do not allocate objects that have no effect (e.g., wrapping a value in a node that evaluates to itself). If an operation is a no-op, skip it.

## NAME THINGS CONCISELY

- Do not prefix every type/function with the domain word when the context already makes it clear. Inside a `password_validator` module, use `Result` — not `PasswordValidationResult`.
- Method and variable names should not repeat information available from the enclosing class, module, or package.

## WRITE MEANINGFUL TESTS

- Test the interesting behavior, not the trivial paths. Prioritize edge cases, error conditions, and concurrency scenarios over "happy path only" coverage.
- Structure test output so that individual failures are identifiable — do not let the first assertion crash the entire suite with no indication of what else passed or failed.
- If the implementation supports a feature (cancellation, composition, error recovery), test it.