l# Expression Parser: Claude vs GPT Code Comparison

## Architectural Overview

Both implementations solve the same problem — a recursive-descent expression parser with variables and functions — but diverge significantly in design philosophy.

**Claude** separates the pipeline into three clean stages: **Tokenizer → AST (via Parser) → Evaluator**, following classic compiler architecture. Each stage is independent and testable.

**GPT** merges tokenization into the parser, operating character-by-character. It also distributes evaluation logic across individual AST node classes rather than centralizing it.

## Why GPT's Code Is Stylistically Weaker

### 1. No Tokenization Phase — Violated Separation of Concerns

Claude produces a `List<Token>` first, then the parser consumes tokens. This is the textbook approach: the tokenizer handles character-level concerns (whitespace, number formats), and the parser deals purely with grammar structure.

GPT's `Parser` class interleaves both responsibilities — `skipWhitespace()` is called before nearly every grammar rule, and `parseNumber()` / `parseIdentifier()` are character-scanning routines embedded inside the parser. This makes the parser harder to read and harder to extend (e.g., adding string literals or new token types would require touching parsing logic).

### 2. Missed Modern Java Features — `sealed` Interfaces and Pattern Matching

Claude defines `sealed interface Expr` with records (`NumberExpr`, `VarExpr`, `BinaryExpr`, etc.) and uses **pattern matching in switch expressions** for evaluation:

```java
return switch (expr) {
    case NumberExpr e -> e.value();
    case VarExpr e -> { ... yield val; }
    case BinaryExpr e -> { ... }
    ...
};
```

This centralizes all evaluation logic in one place and leverages the compiler's exhaustiveness checking — if a new `Expr` variant is added, the compiler forces you to handle it.

GPT uses a plain `interface Node` where every record separately implements `evaluate()`. The logic is scattered across five different classes, and adding a new node type doesn't produce a compile-time warning if you forget to wire it into some other part of the system. For a Java 21 codebase, ignoring `sealed` types and pattern matching is a missed opportunity.

### 3. Over-Engineering and Defensive Noise

GPT adds several layers of defensive coding that are unnecessary given the task scope:

- **Null checks on function registration** (`name == null`, `function == null`) — the task never requires guarding against null registrations.
- **Null-safe input handling** (`input == null ? "" : input`) — no caller passes null.
- **`List.copyOf()` on argument lists** — defensive copying that adds allocation overhead for no safety benefit in this context.
- **Scientific notation parsing** (`e`/`E` handling in `parseNumber`) — not required by the spec.

Claude's code handles exactly what is asked for, no more. This makes it shorter, easier to audit, and less cluttered.

### 4. Verbose Record Boilerplate

Each of GPT's five `Node` records carries a full `@Override public double evaluate(...)` method body. The `FunctionCallNode.evaluate()` alone is 8 lines. Multiply by five node types and you get ~50 lines of evaluation logic spread across the file.

Claude achieves the same in a single 25-line `evaluate` method using pattern matching. The reader sees all evaluation rules at a glance rather than jumping between classes.

### 5. Redundant Unary-Plus Node Allocation

GPT's `parseUnary()` creates `new UnaryNode('+', parseUnary())` for unary `+`, allocating an AST node that does nothing at evaluation time. Claude simply recurses into `unary()` without wrapping, producing a leaner AST.

### 6. Instance-Based API Where Static Suffices

GPT requires `new ExpressionParser()` and stores functions as instance state. For a stateless math evaluator with a shared function registry, Claude's static API (`ExpressionParser.eval(expr, vars)`) is simpler and more natural. The instance-based design suggests an object lifecycle that doesn't actually exist.

### 7. Weaker Test Coverage

Claude's `main` method contains **28 test cases** organized into clear categories (Basic Arithmetic, Precedence, Variables, Functions, Whitespace, Unary Minus, Complex Expressions) with a pass/fail counter and non-zero exit on failure.

GPT's `main` has **9 test cases** with no categorization and uses `AssertionError` (which also contains a typo — should be `AssertionError` → `AssertionError` is actually the correct class name, but the test helper is less informative). The test suite doesn't exercise edge cases like nested parentheses, no-space expressions, tab handling, or multiple chained operations.

## Summary Table

| Aspect           | Claude                                                        | GPT                                                 |
|------------------|---------------------------------------------------------------|-----------------------------------------------------|
| Architecture     | Tokenizer → AST → Evaluator (3 stages)                        | Character-level parser → AST with embedded eval     |
| Java 21 idioms   | `sealed interface`, pattern matching `switch`, `record Token` | Plain `interface`, per-class `evaluate()` overrides |
| Code size        | ~278 lines                                                    | ~338 lines                                          |
| Evaluation logic | Centralized in one `switch` (25 lines)                        | Distributed across 5 record classes (~50 lines)     |
| Defensive coding | Minimal, appropriate                                          | Over-engineered null checks, defensive copies       |
| Function API     | Static, simple                                                | Instance-based, heavier                             |
| Test cases       | 28, categorized, with exit code                               | 9, flat list                                        |
| Extensibility    | Add `Expr` variant → compiler enforces handling               | Add `Node` variant → no compile-time safety net     |

## Conclusion

GPT produced correct, working code, but wrote it in a pre-Java-17 style that ignores the language features the task explicitly permits (Java 21). The merged tokenizer/parser, scattered evaluation logic, and over-defensive patterns result in code that is longer, harder to navigate, and less idiomatic than Claude's implementation. Claude's version reads like a textbook recursive-descent parser — clean separation of phases, modern type system usage, and concise centralized evaluation.