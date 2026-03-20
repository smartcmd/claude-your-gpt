package claude;

import java.util.List;

public record ValidationResult(boolean passed, List<String> failures) {

    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(List<String> failures) {
        return new ValidationResult(false, List.copyOf(failures));
    }
}