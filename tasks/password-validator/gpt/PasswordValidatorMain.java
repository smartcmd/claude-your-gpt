import java.util.List;
import java.util.Objects;

public final class PasswordValidatorMain {
    public static void main(String[] args) {
        testPasswordPassesWhenAllEnabledRulesAreSatisfied();
        testValidationReturnsAllFailureReasons();
        testRulesCanBeDisabled();
        testValidatorCanBeReusedAcrossMultipleCalls();
        testCustomSpecialCharacterSet();
        System.out.println("All password validator tests passed.");
    }

    private static void testPasswordPassesWhenAllEnabledRulesAreSatisfied() {
        PasswordValidator validator = new PasswordValidator(
            PasswordValidationConfig.builder()
                .minimumLength(8)
                .requireUppercase(true)
                .requireDigit(true)
                .requireSpecialCharacter(true)
                .build()
        );

        PasswordValidationResult result = validator.validate("Secure9!");

        assertTrue(result.isValid(), "A password satisfying every enabled rule should pass validation.");
        assertEquals(List.of(), result.failureReasons(), "A valid password should not have failure reasons.");
    }

    private static void testValidationReturnsAllFailureReasons() {
        PasswordValidator validator = new PasswordValidator(
            PasswordValidationConfig.builder()
                .minimumLength(10)
                .requireUppercase(true)
                .requireDigit(true)
                .requireSpecialCharacter(true)
                .build()
        );

        PasswordValidationResult result = validator.validate("weak");

        assertFalse(result.isValid(), "An invalid password should not pass validation.");
        assertEquals(
            List.of(
                "Password must be at least 10 characters long.",
                "Password must contain at least one uppercase letter.",
                "Password must contain at least one digit.",
                "Password must contain at least one special character from: !@#$%^&*"
            ),
            result.failureReasons(),
            "The validator should report every violated rule."
        );
    }

    private static void testRulesCanBeDisabled() {
        PasswordValidator validator = new PasswordValidator(
            PasswordValidationConfig.builder()
                .minimumLength(4)
                .requireUppercase(false)
                .requireDigit(true)
                .requireSpecialCharacter(false)
                .build()
        );

        PasswordValidationResult result = validator.validate("abcd1");

        assertTrue(result.isValid(), "Disabled rules should not be enforced.");
    }

    private static void testValidatorCanBeReusedAcrossMultipleCalls() {
        PasswordValidator validator = new PasswordValidator(
            PasswordValidationConfig.builder()
                .minimumLength(6)
                .requireUppercase(true)
                .requireDigit(true)
                .build()
        );

        PasswordValidationResult firstResult = validator.validate("short");
        PasswordValidationResult secondResult = validator.validate("Longer7");

        assertFalse(firstResult.isValid(), "The first password should fail the configured rules.");
        assertEquals(
            List.of(
                "Password must be at least 6 characters long.",
                "Password must contain at least one uppercase letter.",
                "Password must contain at least one digit."
            ),
            firstResult.failureReasons(),
            "Repeated calls should produce an independent set of failure reasons."
        );
        assertTrue(secondResult.isValid(), "The same validator instance should validate later passwords correctly.");
    }

    private static void testCustomSpecialCharacterSet() {
        PasswordValidator validator = new PasswordValidator(
            PasswordValidationConfig.builder()
                .minimumLength(5)
                .requireSpecialCharacter(true)
                .specialCharacters("?")
                .build()
        );

        PasswordValidationResult failingResult = validator.validate("Hello!");
        PasswordValidationResult passingResult = validator.validate("Hello?");

        assertFalse(failingResult.isValid(), "Only configured special characters should satisfy the rule.");
        assertEquals(
            List.of("Password must contain at least one special character from: ?"),
            failingResult.failureReasons(),
            "The failure message should reflect the configured special characters."
        );
        assertTrue(passingResult.isValid(), "The configured special character should satisfy the rule.");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + System.lineSeparator()
                + "Expected: " + expected + System.lineSeparator()
                + "Actual:   " + actual);
        }
    }
}
