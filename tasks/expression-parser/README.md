# Expression Parser

Implement a system that can parse and evaluate mathematical expressions with variables and simple function calls. Your implementation must support:

1. Basic arithmetic operators

- Addition (`+`)
- Subtraction (`-`)
- Multiplication (`*`)
- Division (`/`)

2. Operator precedence and associativity

- Standard precedence rules (`*` and `/` before `+` and `-`)
- Parentheses `(` and `)` must override precedence

3. Variables

- Expressions may contain variables (e.g., `a + b * 2`)
- Variable values are provided via a `Map<String, Double>`

4. Function calls

- Support simple functions like:
  - `max(a, b)`
  - `min(a, b)`
- You may design the function system in a flexible/extensible way

5. Whitespace tolerance

- Expressions may contain arbitrary spaces

## Requirements

- Using Java 21
- Place the source files in the directory directly. Do not using any build tools like Gradle or Maven
- PLace the test cases in the main method