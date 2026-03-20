import java.util.Objects;

public final class PasswordValidationConfig {
    private final int minimumLength;
    private final boolean requireUppercase;
    private final boolean requireDigit;
    private final boolean requireSpecialCharacter;
    private final String specialCharacters;

    private PasswordValidationConfig(Builder builder) {
        this.minimumLength = builder.minimumLength;
        this.requireUppercase = builder.requireUppercase;
        this.requireDigit = builder.requireDigit;
        this.requireSpecialCharacter = builder.requireSpecialCharacter;
        this.specialCharacters = builder.specialCharacters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int minimumLength() {
        return minimumLength;
    }

    public boolean requireUppercase() {
        return requireUppercase;
    }

    public boolean requireDigit() {
        return requireDigit;
    }

    public boolean requireSpecialCharacter() {
        return requireSpecialCharacter;
    }

    public String specialCharacters() {
        return specialCharacters;
    }

    public static final class Builder {
        private int minimumLength;
        private boolean requireUppercase;
        private boolean requireDigit;
        private boolean requireSpecialCharacter;
        private String specialCharacters = "!@#$%^&*";

        private Builder() {
        }

        public Builder minimumLength(int minimumLength) {
            if (minimumLength < 0) {
                throw new IllegalArgumentException("Minimum length must not be negative.");
            }
            this.minimumLength = minimumLength;
            return this;
        }

        public Builder requireUppercase(boolean requireUppercase) {
            this.requireUppercase = requireUppercase;
            return this;
        }

        public Builder requireDigit(boolean requireDigit) {
            this.requireDigit = requireDigit;
            return this;
        }

        public Builder requireSpecialCharacter(boolean requireSpecialCharacter) {
            this.requireSpecialCharacter = requireSpecialCharacter;
            return this;
        }

        public Builder specialCharacters(String specialCharacters) {
            if (specialCharacters == null || specialCharacters.isEmpty()) {
                throw new IllegalArgumentException("Special characters must not be null or empty.");
            }
            this.specialCharacters = specialCharacters;
            return this;
        }

        public PasswordValidationConfig build() {
            Objects.requireNonNull(specialCharacters, "Special characters must not be null.");
            return new PasswordValidationConfig(this);
        }
    }
}
