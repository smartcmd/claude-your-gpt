package claude;

import java.util.List;
import java.util.Objects;

public class PasswordValidator {

    private final List<ValidationRule> rules;

    public PasswordValidator(List<ValidationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public PasswordValidator(ValidationRule... rules) {
        this(List.of(rules));
    }

    public ValidationResult validate(String password) {
        List<String> failures = rules.stream()
                .map(rule -> rule.check(password))
                .filter(Objects::nonNull)
                .toList();

        return failures.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(failures);
    }

    public static void main(String[] args) {
        // Validator with all rules enabled
        var fullValidator = new PasswordValidator(
                Rules.minLength(8),
                Rules.hasUppercase(),
                Rules.hasDigit(),
                Rules.hasSpecialChar()
        );

        // Validator with only length and digit rules
        var partialValidator = new PasswordValidator(
                Rules.minLength(6),
                Rules.hasDigit()
        );

        // --- Full validator tests ---
        test("Full: valid password", fullValidator.validate("Hello1!abc"),
                true, 0);

        test("Full: too short, no upper, no digit, no special", fullValidator.validate("abc"),
                false, 4);

        test("Full: missing special char", fullValidator.validate("Hello123abcd"),
                false, 1);

        test("Full: missing digit", fullValidator.validate("Hello!abcd"),
                false, 1);

        test("Full: missing uppercase", fullValidator.validate("hello1!abc"),
                false, 1);

        test("Full: only length fails", fullValidator.validate("H1!a"),
                false, 1);

        // --- Partial validator tests ---
        test("Partial: valid", partialValidator.validate("abcdef1"),
                true, 0);

        test("Partial: too short", partialValidator.validate("ab1"),
                false, 1);

        test("Partial: no digit", partialValidator.validate("abcdefgh"),
                false, 1);

        // --- Reusability: same validator, multiple calls ---
        var result1 = fullValidator.validate("bad");
        var result2 = fullValidator.validate("GoodPass1!");
        test("Reuse: first call fails", result1, false, 4);
        test("Reuse: second call passes", result2, true, 0);

        System.out.println("\nAll tests passed.");
    }

    private static void test(String name, ValidationResult result,
                             boolean expectPassed, int expectFailureCount) {
        if (result.passed() != expectPassed) {
            throw new AssertionError(name + ": expected passed=" + expectPassed
                    + " but got " + result.passed() + " " + result.failures());
        }
        if (result.failures().size() != expectFailureCount) {
            throw new AssertionError(name + ": expected " + expectFailureCount
                    + " failures but got " + result.failures().size()
                    + " " + result.failures());
        }
        System.out.println("PASS: " + name);
    }
}