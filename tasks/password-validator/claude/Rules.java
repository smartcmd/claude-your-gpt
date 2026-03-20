package claude;

public final class Rules {

    private Rules() {}

    public static ValidationRule minLength(int min) {
        return password -> password.length() < min
                ? "Must be at least " + min + " characters"
                : null;
    }

    public static ValidationRule hasUppercase() {
        return password -> password.chars().anyMatch(Character::isUpperCase)
                ? null
                : "Must contain at least one uppercase letter";
    }

    public static ValidationRule hasDigit() {
        return password -> password.chars().anyMatch(Character::isDigit)
                ? null
                : "Must contain at least one digit";
    }

    public static ValidationRule hasSpecialChar() {
        return password -> {
            String specials = "!@#$%^&*";
            for (char c : password.toCharArray()) {
                if (specials.indexOf(c) >= 0) return null;
            }
            return "Must contain at least one special character (!@#$%^&*)";
        };
    }
}