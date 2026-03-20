package claude;

@FunctionalInterface
public interface ValidationRule {

    String check(String password);
}