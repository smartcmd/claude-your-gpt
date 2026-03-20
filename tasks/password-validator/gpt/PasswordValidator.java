import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PasswordValidator {
    private final PasswordValidationConfig config;

    public PasswordValidator(PasswordValidationConfig config) {
        this.config = Objects.requireNonNull(config, "Config must not be null.");
    }

    public PasswordValidationResult validate(String password) {
        List<String> failureReasons = new ArrayList<>();

        if (password == null) {
            failureReasons.add("Password must not be null.");
            return PasswordValidationResult.failed(failureReasons);
        }

        if (password.length() < config.minimumLength()) {
            failureReasons.add("Password must be at least " + config.minimumLength() + " characters long.");
        }

        if (config.requireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            failureReasons.add("Password must contain at least one uppercase letter.");
        }

        if (config.requireDigit() && password.chars().noneMatch(Character::isDigit)) {
            failureReasons.add("Password must contain at least one digit.");
        }

        if (config.requireSpecialCharacter() && password.chars().noneMatch(this::isConfiguredSpecialCharacter)) {
            failureReasons.add(
                "Password must contain at least one special character from: " + config.specialCharacters()
            );
        }

        return failureReasons.isEmpty()
            ? PasswordValidationResult.passed()
            : PasswordValidationResult.failed(failureReasons);
    }

    private boolean isConfiguredSpecialCharacter(int value) {
        return config.specialCharacters().indexOf(value) >= 0;
    }
}
