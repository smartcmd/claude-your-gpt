import java.util.List;

public final class PasswordValidationResult {
    private final boolean valid;
    private final List<String> failureReasons;

    private PasswordValidationResult(boolean valid, List<String> failureReasons) {
        this.valid = valid;
        this.failureReasons = List.copyOf(failureReasons);
    }

    public static PasswordValidationResult passed() {
        return new PasswordValidationResult(true, List.of());
    }

    public static PasswordValidationResult failed(List<String> failureReasons) {
        if (failureReasons == null || failureReasons.isEmpty()) {
            throw new IllegalArgumentException("Failure reasons must not be null or empty for a failed result.");
        }
        return new PasswordValidationResult(false, failureReasons);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> failureReasons() {
        return failureReasons;
    }
}
