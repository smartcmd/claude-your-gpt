import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExpressionParser {
    private final Map<String, ExpressionFunction> functions = new HashMap<>();

    public ExpressionParser() {
        registerFunction("max", arguments -> {
            requireMinimumArguments("max", arguments, 1);
            double result = arguments.getFirst();
            for (int i = 1; i < arguments.size(); i++) {
                result = Math.max(result, arguments.get(i));
            }
            return result;
        });

        registerFunction("min", arguments -> {
            requireMinimumArguments("min", arguments, 1);
            double result = arguments.getFirst();
            for (int i = 1; i < arguments.size(); i++) {
                result = Math.min(result, arguments.get(i));
            }
            return result;
        });
    }

    public void registerFunction(String name, ExpressionFunction function) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Function name must not be blank.");
        }
        if (function == null) {
            throw new IllegalArgumentException("Function implementation must not be null.");
        }
        functions.put(name, function);
    }

    public double evaluate(String expression, Map<String, Double> variables) {
        Parser parser = new Parser(expression);
        Node root = parser.parseExpression();
        parser.expectEnd();
        return root.evaluate(variables == null ? Map.of() : variables, functions);
    }

    private static void requireMinimumArguments(String functionName, List<Double> arguments, int minimum) {
        if (arguments.size() < minimum) {
            throw new IllegalArgumentException(
                "Function '" + functionName + "' requires at least " + minimum + " argument(s)."
            );
        }
    }

    @FunctionalInterface
    public interface ExpressionFunction {
        double apply(List<Double> arguments);
    }

    private interface Node {
        double evaluate(Map<String, Double> variables, Map<String, ExpressionFunction> functions);
    }

    private record NumberNode(double value) implements Node {
        @Override
        public double evaluate(Map<String, Double> variables, Map<String, ExpressionFunction> functions) {
            return value;
        }
    }

    private record VariableNode(String name) implements Node {
        @Override
        public double evaluate(Map<String, Double> variables, Map<String, ExpressionFunction> functions) {
            Double value = variables.get(name);
            if (value == null) {
                throw new IllegalArgumentException("Unknown variable: " + name);
            }
            return value;
        }
    }

    private record UnaryNode(char operator, Node operand) implements Node {
        @Override
        public double evaluate(Map<String, Double> variables, Map<String, ExpressionFunction> functions) {
            double value = operand.evaluate(variables, functions);
            return operator == '-' ? -value : value;
        }
    }

    private record BinaryNode(char operator, Node left, Node right) implements Node {
        @Override
        public double evaluate(Map<String, Double> variables, Map<String, ExpressionFunction> functions) {
            double leftValue = left.evaluate(variables, functions);
            double rightValue = right.evaluate(variables, functions);
            return switch (operator) {
                case '+' -> leftValue + rightValue;
                case '-' -> leftValue - rightValue;
                case '*' -> leftValue * rightValue;
                case '/' -> leftValue / rightValue;
                default -> throw new IllegalStateException("Unsupported operator: " + operator);
            };
        }
    }

    private record FunctionCallNode(String name, List<Node> arguments) implements Node {
        @Override
        public double evaluate(Map<String, Double> variables, Map<String, ExpressionFunction> functions) {
            ExpressionFunction function = functions.get(name);
            if (function == null) {
                throw new IllegalArgumentException("Unknown function: " + name);
            }

            List<Double> values = new ArrayList<>(arguments.size());
            for (Node argument : arguments) {
                values.add(argument.evaluate(variables, functions));
            }
            return function.apply(List.copyOf(values));
        }
    }

    private static final class Parser {
        private final String input;
        private int position;

        private Parser(String input) {
            this.input = input == null ? "" : input;
        }

        private Node parseExpression() {
            Node node = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    node = new BinaryNode('+', node, parseTerm());
                } else if (match('-')) {
                    node = new BinaryNode('-', node, parseTerm());
                } else {
                    return node;
                }
            }
        }

        private Node parseTerm() {
            Node node = parseUnary();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    node = new BinaryNode('*', node, parseUnary());
                } else if (match('/')) {
                    node = new BinaryNode('/', node, parseUnary());
                } else {
                    return node;
                }
            }
        }

        private Node parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return new UnaryNode('+', parseUnary());
            }
            if (match('-')) {
                return new UnaryNode('-', parseUnary());
            }
            return parsePrimary();
        }

        private Node parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                Node inner = parseExpression();
                skipWhitespace();
                expect(')');
                return inner;
            }

            if (isDigit(peek()) || peek() == '.') {
                return new NumberNode(parseNumber());
            }

            if (isIdentifierStart(peek())) {
                String identifier = parseIdentifier();
                skipWhitespace();
                if (match('(')) {
                    return new FunctionCallNode(identifier, parseArguments());
                }
                return new VariableNode(identifier);
            }

            throw error("Unexpected token.");
        }

        private List<Node> parseArguments() {
            List<Node> arguments = new ArrayList<>();
            skipWhitespace();
            if (match(')')) {
                return arguments;
            }

            do {
                arguments.add(parseExpression());
                skipWhitespace();
            } while (match(','));

            expect(')');
            return List.copyOf(arguments);
        }

        private double parseNumber() {
            int start = position;

            while (isDigit(peek())) {
                position++;
            }
            if (peek() == '.') {
                position++;
                while (isDigit(peek())) {
                    position++;
                }
            }
            if (peek() == 'e' || peek() == 'E') {
                position++;
                if (peek() == '+' || peek() == '-') {
                    position++;
                }
                if (!isDigit(peek())) {
                    throw error("Invalid number format.");
                }
                while (isDigit(peek())) {
                    position++;
                }
            }

            try {
                return Double.parseDouble(input.substring(start, position));
            } catch (NumberFormatException exception) {
                throw error("Invalid number format.");
            }
        }

        private String parseIdentifier() {
            int start = position;
            position++;
            while (isIdentifierPart(peek())) {
                position++;
            }
            return input.substring(start, position);
        }

        private void expectEnd() {
            skipWhitespace();
            if (position != input.length()) {
                throw error("Unexpected trailing input.");
            }
        }

        private void expect(char expected) {
            if (!match(expected)) {
                throw error("Expected '" + expected + "'.");
            }
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (peek() == expected) {
                position++;
                return true;
            }
            return false;
        }

        private char peek() {
            return position < input.length() ? input.charAt(position) : '\0';
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private boolean isDigit(char value) {
            return value >= '0' && value <= '9';
        }

        private boolean isIdentifierStart(char value) {
            return Character.isLetter(value) || value == '_';
        }

        private boolean isIdentifierPart(char value) {
            return Character.isLetterOrDigit(value) || value == '_';
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " At position " + position + " in \"" + input + "\"");
        }
    }

    private static void assertClose(String name, double actual, double expected) {
        double delta = Math.abs(actual - expected);
        if (delta > 1e-9) {
            throw new AssertionError(
                name + " failed. Expected " + expected + " but was " + actual
            );
        }
        System.out.println("[PASS] " + name + " = " + actual);
    }

    public static void main(String[] args) {
        ExpressionParser parser = new ExpressionParser();
        parser.registerFunction("avg", arguments -> {
            requireMinimumArguments("avg", arguments, 1);
            double sum = 0.0;
            for (double value : arguments) {
                sum += value;
            }
            return sum / arguments.size();
        });

        Map<String, Double> variables = Map.of(
            "a", 3.0,
            "b", 4.0,
            "x", 9.0,
            "y", 1.0
        );

        assertClose("multiplication precedence", parser.evaluate("1 + 2 * 3", variables), 7.0);
        assertClose("parentheses override precedence", parser.evaluate("(1 + 2) * 3", variables), 9.0);
        assertClose("left associative subtraction", parser.evaluate("10 - 6 - 1", variables), 3.0);
        assertClose("left associative division", parser.evaluate("8 / 2 / 2", variables), 2.0);
        assertClose("variables", parser.evaluate("a + b * 2", variables), 11.0);
        assertClose("whitespace tolerance", parser.evaluate("  a   +   b * 2  ", variables), 11.0);
        assertClose("nested functions", parser.evaluate("max(2, min(x, 5)) + y", variables), 6.0);
        assertClose("custom function", parser.evaluate("avg(2, 4, 6, 8)", variables), 5.0);
        assertClose("unary minus", parser.evaluate("-a + 10", variables), 7.0);

        System.out.println("All expression parser tests passed.");
    }
}
