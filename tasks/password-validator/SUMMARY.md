# Password Validator: Claude vs GPT Code Comparison

## Architecture Overview

| Aspect        | Claude                                                         | GPT                                                                                              |
|---------------|----------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| Files         | 4 (PasswordValidator, Rules, ValidationRule, ValidationResult) | 4 (PasswordValidator, PasswordValidationConfig, PasswordValidationResult, PasswordValidatorMain) |
| Core design   | Strategy pattern via `@FunctionalInterface`                    | Builder-configured if-chain                                                                      |
| Result type   | `record`                                                       | Manual immutable class                                                                           |
| Extensibility | Open (add any `ValidationRule` lambda)                         | Closed (must edit `PasswordValidator` to add rules)                                              |

## Why GPT's Code Falls Short Stylistically

### 1. Over-engineered Configuration, Under-engineered Abstraction

GPT pours 88 lines into a `PasswordValidationConfig` with a full Builder pattern — `Builder` inner class, fluent setters, validation on every field — yet the actual rules are **hardcoded as if-statements** inside `PasswordValidator.validate()`. The Builder gives the illusion of flexibility while the validator itself is rigid. Adding a new rule (e.g., "no repeating characters") means modifying the validator class, violating the Open/Closed Principle.

Claude invests those lines in the right place: a `@FunctionalInterface` (`ValidationRule`) and a `Rules` factory. Adding a rule is a one-liner lambda — no existing code changes needed.

### 2. Closed-World Assumption

GPT's design bakes in a fixed set of rules (`requireUppercase`, `requireDigit`, `requireSpecialCharacter`) as boolean flags. The caller can toggle them on/off but **cannot define new rules**. This is configuration, not configurability. The task asked for "configurable validation rules" and "togglable rules" — GPT interpreted this as a closed enum of booleans rather than a composable rule set.

Claude treats rules as first-class values. The caller composes any combination of rules, including custom ones, at construction time. Toggling a rule means simply not including it in the list.

### 3. Boilerplate-Heavy Result Class

GPT's `PasswordValidationResult` is 30 lines of manually written immutable class: private constructor, static factories, getters, `List.copyOf`, defensive checks. Claude achieves the same in 5 lines with a Java `record` — which provides immutability, accessors, `equals`/`hashCode`/`toString` for free. Using a record here is not just shorter; it signals intent ("this is pure data") more clearly than a handwritten class.

### 4. Verbose Naming

GPT prefixes everything with `Password`: `PasswordValidationConfig`, `PasswordValidationResult`, `PasswordValidatorMain`. This adds no information — these classes already live in a password-validator context. Claude uses concise names (`ValidationRule`, `ValidationResult`, `Rules`) that are descriptive without being redundant.

### 5. Defensive Coding to a Fault

GPT adds null checks and precondition validations everywhere: `Objects.requireNonNull(config)`, null-check on password input, `IllegalArgumentException` if special characters are null/empty, redundant `Objects.requireNonNull` in `build()` after the field is already initialized with a default. While defensive programming has its place, here it produces noise — the prompt never asked for null-safety, and the extra guards obscure the core logic rather than protecting against realistic failure modes.

Claude trusts its own internal boundaries and keeps the code focused on the actual task.

### 6. Test Separation vs Integration

GPT places tests in a separate `PasswordValidatorMain` class with custom `assertTrue`/`assertFalse`/`assertEquals` helpers — 135 lines of test infrastructure. Claude embeds tests directly in `main` with a compact `test()` helper, keeping everything self-contained and easy to scan. For a task that explicitly says "place test cases in the main method," Claude's approach is both more compliant and more concise.

## Summary

GPT's code is correct and well-structured by enterprise Java standards, but it mistakes **ceremony for quality**. It reaches for heavyweight patterns (Builder, manual immutable classes, exhaustive null checks) that add volume without adding value for the scope of this task. Claude's code demonstrates that good design often means choosing the lightest abstraction that solves the problem — a functional interface over a config object, a record over a handwritten class, composition over boolean flags.