import java.util.*;
import java.util.function.Function;

public class ExpressionParser {

    enum TokenType {
        NUMBER, IDENTIFIER, PLUS, MINUS, STAR, SLASH, LPAREN, RPAREN, COMMA, EOF
    }

    record Token(TokenType type, String value) {}

    // --- Tokenizer ---

    static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < input.length() && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) i++;
                tokens.add(new Token(TokenType.NUMBER, input.substring(start, i)));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < input.length() && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) i++;
                tokens.add(new Token(TokenType.IDENTIFIER, input.substring(start, i)));
            } else {
                TokenType type = switch (c) {
                    case '+' -> TokenType.PLUS;
                    case '-' -> TokenType.MINUS;
                    case '*' -> TokenType.STAR;
                    case '/' -> TokenType.SLASH;
                    case '(' -> TokenType.LPAREN;
                    case ')' -> TokenType.RPAREN;
                    case ',' -> TokenType.COMMA;
                    default -> throw new RuntimeException("Unexpected character: " + c);
                };
                tokens.add(new Token(type, String.valueOf(c)));
                i++;
            }
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    // --- AST ---

    sealed interface Expr {}
    record NumberExpr(double value) implements Expr {}
    record VarExpr(String name) implements Expr {}
    record BinaryExpr(String op, Expr left, Expr right) implements Expr {}
    record UnaryExpr(String op, Expr operand) implements Expr {}
    record FuncCallExpr(String name, List<Expr> args) implements Expr {}

    // --- Recursive Descent Parser ---

    static class Parser {
        private final List<Token> tokens;
        private int pos;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.pos = 0;
        }

        private Token peek() { return tokens.get(pos); }
        private Token advance() { return tokens.get(pos++); }

        private Token expect(TokenType type) {
            Token t = advance();
            if (t.type() != type) throw new RuntimeException("Expected " + type + " but got " + t);
            return t;
        }

        Expr parse() {
            Expr expr = expression();
            expect(TokenType.EOF);
            return expr;
        }

        // expression -> term (('+' | '-') term)*
        private Expr expression() {
            Expr left = term();
            while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
                String op = advance().value();
                left = new BinaryExpr(op, left, term());
            }
            return left;
        }

        // term -> unary (('*' | '/') unary)*
        private Expr term() {
            Expr left = unary();
            while (peek().type() == TokenType.STAR || peek().type() == TokenType.SLASH) {
                String op = advance().value();
                left = new BinaryExpr(op, left, unary());
            }
            return left;
        }

        // unary -> '-' unary | '+' unary | primary
        private Expr unary() {
            if (peek().type() == TokenType.MINUS) {
                advance();
                return new UnaryExpr("-", unary());
            }
            if (peek().type() == TokenType.PLUS) {
                advance();
                return unary();
            }
            return primary();
        }

        // primary -> NUMBER | IDENTIFIER ('(' args ')')? | '(' expression ')'
        private Expr primary() {
            Token t = peek();
            if (t.type() == TokenType.NUMBER) {
                return new NumberExpr(Double.parseDouble(advance().value()));
            }
            if (t.type() == TokenType.IDENTIFIER) {
                String name = advance().value();
                if (peek().type() == TokenType.LPAREN) {
                    advance();
                    List<Expr> args = new ArrayList<>();
                    if (peek().type() != TokenType.RPAREN) {
                        args.add(expression());
                        while (peek().type() == TokenType.COMMA) {
                            advance();
                            args.add(expression());
                        }
                    }
                    expect(TokenType.RPAREN);
                    return new FuncCallExpr(name, args);
                }
                return new VarExpr(name);
            }
            if (t.type() == TokenType.LPAREN) {
                advance();
                Expr expr = expression();
                expect(TokenType.RPAREN);
                return expr;
            }
            throw new RuntimeException("Unexpected token: " + t);
        }
    }

    // --- Built-in Functions (extensible) ---

    private static final Map<String, Function<List<Double>, Double>> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.put("max", args -> {
            if (args.size() != 2) throw new RuntimeException("max expects 2 arguments");
            return Math.max(args.get(0), args.get(1));
        });
        FUNCTIONS.put("min", args -> {
            if (args.size() != 2) throw new RuntimeException("min expects 2 arguments");
            return Math.min(args.get(0), args.get(1));
        });
    }

    public static void registerFunction(String name, Function<List<Double>, Double> fn) {
        FUNCTIONS.put(name, fn);
    }

    // --- Evaluator ---

    static double evaluate(Expr expr, Map<String, Double> variables) {
        return switch (expr) {
            case NumberExpr e -> e.value();
            case VarExpr e -> {
                Double val = variables.get(e.name());
                if (val == null) throw new RuntimeException("Undefined variable: " + e.name());
                yield val;
            }
            case BinaryExpr e -> {
                double l = evaluate(e.left(), variables);
                double r = evaluate(e.right(), variables);
                yield switch (e.op()) {
                    case "+" -> l + r;
                    case "-" -> l - r;
                    case "*" -> l * r;
                    case "/" -> l / r;
                    default -> throw new RuntimeException("Unknown operator: " + e.op());
                };
            }
            case UnaryExpr e -> e.op().equals("-") ? -evaluate(e.operand(), variables) : evaluate(e.operand(), variables);
            case FuncCallExpr e -> {
                var fn = FUNCTIONS.get(e.name());
                if (fn == null) throw new RuntimeException("Unknown function: " + e.name());
                yield fn.apply(e.args().stream().map(a -> evaluate(a, variables)).toList());
            }
        };
    }

    // --- Public API ---

    public static double eval(String expression, Map<String, Double> variables) {
        return evaluate(new Parser(tokenize(expression)).parse(), variables);
    }

    public static double eval(String expression) {
        return eval(expression, Map.of());
    }

    // --- Tests ---

    private static int passed = 0, failed = 0;

    private static void test(String name, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            System.out.printf("  PASS  %s%n", name);
            passed++;
        } else {
            System.out.printf("  FAIL  %s  (expected=%g, actual=%g)%n", name, expected, actual);
            failed++;
        }
    }

    public static void main(String[] args) {
        Map<String, Double> vars = Map.of("a", 10.0, "b", 3.0, "x", 5.0);

        System.out.println("=== Basic Arithmetic ===");
        test("2 + 3 = 5", 5, eval("2 + 3"));
        test("10 - 4 = 6", 6, eval("10 - 4"));
        test("2 * 3 = 6", 6, eval("2 * 3"));
        test("7 / 2 = 3.5", 3.5, eval("7 / 2"));
        test("1.5 + 2.5 = 4", 4, eval("1.5 + 2.5"));

        System.out.println("\n=== Operator Precedence & Parentheses ===");
        test("1 + 2 * 3 = 7", 7, eval("1 + 2 * 3"));
        test("6 - 4 / 2 = 4", 4, eval("6 - 4 / 2"));
        test("(1 + 2) * 3 = 9", 9, eval("(1 + 2) * 3"));
        test("2 * (3 + 4) = 14", 14, eval("2 * (3 + 4)"));
        test("((2 + 3)) = 5", 5, eval("((2 + 3))"));
        test("(2 + 3) * (4 - 1) = 15", 15, eval("(2 + 3) * (4 - 1)"));
        test("2 + 3 * 4 - 1 = 13", 13, eval("2 + 3 * 4 - 1"));

        System.out.println("\n=== Variables ===");
        test("a = 10", 10, eval("a", vars));
        test("a + b = 13", 13, eval("a + b", vars));
        test("a + b * 2 = 16", 16, eval("a + b * 2", vars));
        test("a * x = 50", 50, eval("a * x", vars));
        test("(a - b) * x = 35", 35, eval("(a - b) * x", vars));

        System.out.println("\n=== Function Calls ===");
        test("max(3, 5) = 5", 5, eval("max(3, 5)"));
        test("min(2, 7) = 2", 2, eval("min(2, 7)"));
        test("max(a, b) = 10", 10, eval("max(a, b)", vars));
        test("min(a, b) = 3", 3, eval("min(a, b)", vars));
        test("max(3,5) + min(2,7) = 7", 7, eval("max(3, 5) + min(2, 7)"));
        test("max(min(3,5), 7) = 7", 7, eval("max(min(3, 5), 7)"));
        test("1 + max(2, 3) * 2 = 7", 7, eval("1 + max(2, 3) * 2"));

        System.out.println("\n=== Whitespace Tolerance ===");
        test("no spaces: 2+3 = 5", 5, eval("2+3"));
        test("extra spaces: ' 2 + 3 ' = 5", 5, eval("  2  +  3  "));
        test("spaces in func: 'max( 3 , 5 )' = 5", 5, eval("max( 3 , 5 )"));
        test("tabs and spaces", 10, eval("\t 5\t* \t2\t"));

        System.out.println("\n=== Unary Minus ===");
        test("-3 = -3", -3, eval("-3"));
        test("-3 + 5 = 2", 2, eval("-3 + 5"));
        test("-(2 + 3) = -5", -5, eval("-(2 + 3)"));
        test("2 * -3 = -6", -6, eval("2 * -3"));

        System.out.println("\n=== Complex Expressions ===");
        test("max(a,b) + min(1,2) = 11", 11, eval("max(a, b) + min(1, 2)", vars));
        test("a * 2 + b = 23", 23, eval("a * 2 + b", vars));
        test("(a + b) * 2 - x = 21", 21, eval("(a + b) * 2 - x", vars));
        test("max(a * 2, b + x) / 4 = 5", 5, eval("max(a * 2, b + x) / 4", vars));

        System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
        if (failed > 0) System.exit(1);
    }
}