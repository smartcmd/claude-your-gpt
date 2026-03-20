# Code Style Requirements

You must follow these style rules strictly:

## DO NOT OVER-ABSTRACT

- If one class only uses a type internally, make it a private nested record/class — not a top-level public file.
- Do not create an interface that has exactly one implementation. That is not abstraction, it is an indirection.
- Do not use the builder pattern unless the object has 5+ required fields or genuinely complex construction. For simple config, use constructor parameters or a factory method.
- Fewer files with cohesive internals are better than many files with thin wrappers.

## DO NOT OVER-DEFEND

- Only add null checks at true system boundaries — public API entry points that accept external, untrusted input.
- Do not null-check parameters in internal/private methods, constructors called only by your own code, or test helpers.
- Do not add defensive copies unless the object is genuinely shared across trust boundaries.
- Omitting a null check is not a bug — it is a deliberate signal that the caller is trusted.