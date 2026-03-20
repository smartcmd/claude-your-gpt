# Password Validator

Implement a configurable password validation system. Your implementation must support:

1. Configurable validation rules

- Minimum length
- Must contain at least one uppercase letter
- Must contain at least one digit
- Must contain at least one special character (e.g. `!@#$%^&*`)

2. Validation result

- Do not return a plain `boolean`
- Return a result object that indicates whether the password passed, and if not, includes a list of all the reasons it failed

3. Rule configurability

- Rules should be togglable — the caller can choose which rules to enforce
- The set of active rules should be configurable at the time the validator is constructed or configured

4. Multiple passwords

- The validator should be reusable across multiple `validate()` calls without being rebuilt

## Requirements

- Using Java 21
- Place the source files in the directory directly. Do not using any build tools like Gradle or Maven
- PLace the test cases in the main method