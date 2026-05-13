package com.rpa.manage.common.util;

import com.rpa.manage.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LimitedFormulaEvaluator {

    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile(
            "^\\s*([\\p{L}_][\\p{L}\\p{N}_]*)\\s*=\\s*(.+)$",
            Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern SUM_PATTERN = Pattern.compile(
            "^sum\\((?<list>[\\p{L}\\p{N}_.]+)\\s+where\\s+(?<condition>.+)\\)\\s*\\.\\s*(?<field>[\\p{L}\\p{N}_]+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    public FormulaResult evaluateIndicatorFormula(String formula, Map<String, Object> context, String fallbackResultName) {
        ParsedFormula parsedFormula = parseFormula(formula, fallbackResultName);
        Object value = evaluateCalculation(parsedFormula.expression(), context);
        return new FormulaResult(parsedFormula.resultName(), value);
    }

    public Object evaluateCalculation(String expression, Map<String, Object> context) {
        if (!StringUtils.hasText(expression)) {
            throw new BusinessException("计算公式不能为空");
        }
        String normalized = expression.trim();
        Matcher sumMatcher = SUM_PATTERN.matcher(normalized);
        if (sumMatcher.matches()) {
            return evaluateSum(sumMatcher, context);
        }
        return new Parser(tokenize(normalized), context).parse();
    }

    public boolean evaluateCondition(String expression, Map<String, Object> context) {
        if (!StringUtils.hasText(expression) || "else".equalsIgnoreCase(expression.trim())) {
            return true;
        }
        return toBoolean(new Parser(tokenize(expression), context).parse());
    }

    public String extractAssignmentName(String formula) {
        if (!StringUtils.hasText(formula)) {
            return null;
        }
        Matcher matcher = ASSIGNMENT_PATTERN.matcher(formula.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }

    private ParsedFormula parseFormula(String formula, String fallbackResultName) {
        if (!StringUtils.hasText(formula)) {
            throw new BusinessException("计算公式不能为空");
        }
        Matcher matcher = ASSIGNMENT_PATTERN.matcher(formula.trim());
        if (matcher.matches()) {
            return new ParsedFormula(matcher.group(1), matcher.group(2));
        }
        if (!StringUtils.hasText(fallbackResultName)) {
            throw new BusinessException("未配置结果变量名，公式也没有赋值变量");
        }
        return new ParsedFormula(fallbackResultName.trim(), formula.trim());
    }

    private BigDecimal evaluateSum(Matcher matcher, Map<String, Object> context) {
        Object source = resolvePath(context, matcher.group("list"));
        if (!(source instanceof List<?> rows)) {
            throw new BusinessException("sum 函数的数据源必须是数组：" + matcher.group("list"));
        }

        String condition = matcher.group("condition");
        String field = matcher.group("field");
        BigDecimal total = BigDecimal.ZERO;
        for (Object row : rows) {
            Map<String, Object> rowContext = new LinkedHashMap<>(context);
            if (row instanceof Map<?, ?> rowMap) {
                rowMap.forEach((key, value) -> rowContext.put(String.valueOf(key), value));
            } else {
                rowContext.put("value", row);
            }
            if (evaluateCondition(condition, rowContext)) {
                total = total.add(toBigDecimal(resolveField(row, field)));
            }
        }
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    private Object resolveField(Object row, String field) {
        if (!(row instanceof Map<?, ?> rowMap)) {
            return row;
        }
        if (rowMap.containsKey(field)) {
            return rowMap.get(field);
        }
        if ("jshj".equals(field) && rowMap.containsKey("jshjText")) {
            return rowMap.get("jshjText");
        }
        throw new BusinessException("字段不存在：" + field);
    }

    private Object resolvePath(Map<String, Object> context, String path) {
        String[] segments = path.split("\\.");
        Object current = context.get(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(segments[i]);
        }
        return current;
    }

    private List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < expression.length()) {
            char ch = expression.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            if (Character.isDigit(ch) || ch == '.') {
                int start = index;
                index++;
                while (index < expression.length()) {
                    char next = expression.charAt(index);
                    if (!Character.isDigit(next) && next != ',' && next != '.') {
                        break;
                    }
                    index++;
                }
                String text = expression.substring(start, index).replace(",", "");
                if (index < expression.length() && expression.charAt(index) == '%') {
                    index++;
                    tokens.add(new Token(TokenType.NUMBER, new BigDecimal(text).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP).toPlainString()));
                } else {
                    tokens.add(new Token(TokenType.NUMBER, text));
                }
                continue;
            }
            if (ch == '\'' || ch == '"') {
                char quote = ch;
                int start = ++index;
                StringBuilder value = new StringBuilder();
                while (index < expression.length() && expression.charAt(index) != quote) {
                    if (expression.charAt(index) == '\\' && index + 1 < expression.length()) {
                        index++;
                    }
                    value.append(expression.charAt(index++));
                }
                if (index >= expression.length()) {
                    throw new BusinessException("字符串缺少结束引号：" + expression.substring(start - 1));
                }
                index++;
                tokens.add(new Token(TokenType.STRING, value.toString()));
                continue;
            }
            if (isIdentifierStart(ch)) {
                int start = index;
                index++;
                while (index < expression.length() && isIdentifierPart(expression.charAt(index))) {
                    index++;
                }
                tokens.add(new Token(TokenType.IDENTIFIER, expression.substring(start, index)));
                continue;
            }
            String two = index + 1 < expression.length() ? expression.substring(index, index + 2) : "";
            if (List.of(">=", "<=", "==", "!=", "&&", "||").contains(two)) {
                tokens.add(new Token(TokenType.OPERATOR, two));
                index += 2;
                continue;
            }
            if ("+-*/()><=.".indexOf(ch) >= 0) {
                tokens.add(new Token(ch == '(' ? TokenType.LEFT_PAREN : ch == ')' ? TokenType.RIGHT_PAREN : TokenType.OPERATOR, String.valueOf(ch)));
                index++;
                continue;
            }
            throw new BusinessException("公式中存在不支持的字符：" + ch);
        }
        tokens.add(new Token(TokenType.END, ""));
        return tokens;
    }

    private boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = String.valueOf(value).trim().replace(",", "");
        if (!StringUtils.hasText(text)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new BusinessException("数值解析失败：" + value);
        }
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0D;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public record FormulaResult(String resultName, Object value) {
    }

    private record ParsedFormula(String resultName, String expression) {
    }

    private record Token(TokenType type, String text) {
    }

    private enum TokenType {
        NUMBER,
        STRING,
        IDENTIFIER,
        OPERATOR,
        LEFT_PAREN,
        RIGHT_PAREN,
        END
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final Map<String, Object> context;
        private int index;

        private Parser(List<Token> tokens, Map<String, Object> context) {
            this.tokens = tokens;
            this.context = context;
        }

        private Object parse() {
            Object value = parseOr();
            expect(TokenType.END, null);
            return value;
        }

        private Object parseOr() {
            Object left = parseAnd();
            while (matchOperator("||") || matchKeyword("or")) {
                Object right = parseAnd();
                left = toBoolean(left) || toBoolean(right);
            }
            return left;
        }

        private Object parseAnd() {
            Object left = parseComparison();
            while (matchOperator("&&") || matchKeyword("and")) {
                Object right = parseComparison();
                left = toBoolean(left) && toBoolean(right);
            }
            return left;
        }

        private Object parseComparison() {
            Object left = parseAdd();
            if (matchKeyword("between")) {
                BigDecimal min = toBigDecimal(parseAdd());
                expectKeyword("and");
                BigDecimal max = toBigDecimal(parseAdd());
                BigDecimal current = toBigDecimal(left);
                return current.compareTo(min) >= 0 && current.compareTo(max) <= 0;
            }
            if (peek(TokenType.OPERATOR, ">") || peek(TokenType.OPERATOR, ">=")
                    || peek(TokenType.OPERATOR, "<") || peek(TokenType.OPERATOR, "<=")
                    || peek(TokenType.OPERATOR, "==") || peek(TokenType.OPERATOR, "=") || peek(TokenType.OPERATOR, "!=")) {
                String operator = consume().text();
                Object right = parseAdd();
                return compare(left, operator, right);
            }
            return left;
        }

        private Object parseAdd() {
            Object left = parseMultiply();
            while (peek(TokenType.OPERATOR, "+") || peek(TokenType.OPERATOR, "-")) {
                String operator = consume().text();
                BigDecimal right = toBigDecimal(parseMultiply());
                BigDecimal current = toBigDecimal(left);
                left = "+".equals(operator) ? current.add(right) : current.subtract(right);
            }
            return left;
        }

        private Object parseMultiply() {
            Object left = parseUnary();
            while (peek(TokenType.OPERATOR, "*") || peek(TokenType.OPERATOR, "/")) {
                String operator = consume().text();
                BigDecimal right = toBigDecimal(parseUnary());
                BigDecimal current = toBigDecimal(left);
                left = "*".equals(operator)
                        ? current.multiply(right)
                        : current.divide(right, 10, RoundingMode.HALF_UP);
            }
            return left;
        }

        private Object parseUnary() {
            if (matchOperator("-")) {
                return toBigDecimal(parseUnary()).negate();
            }
            return parsePrimary();
        }

        private Object parsePrimary() {
            if (peek(TokenType.NUMBER, null)) {
                return new BigDecimal(consume().text());
            }
            if (peek(TokenType.STRING, null)) {
                return consume().text();
            }
            if (peek(TokenType.IDENTIFIER, null)) {
                String identifier = consume().text();
                if ("true".equalsIgnoreCase(identifier)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(identifier)) {
                    return false;
                }
                Object value = resolveIdentifier(identifier);
                if (value == null) {
                    throw new BusinessException("公式变量不存在：" + identifier);
                }
                return value;
            }
            if (match(TokenType.LEFT_PAREN, null)) {
                Object value = parseOr();
                expect(TokenType.RIGHT_PAREN, null);
                return value;
            }
            throw new BusinessException("公式语法错误，位置：" + index);
        }

        private Object resolveIdentifier(String identifier) {
            String[] parts = identifier.split("\\.");
            Object current = context.get(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (!(current instanceof Map<?, ?> currentMap)) {
                    return null;
                }
                current = currentMap.get(parts[i]);
            }
            return current;
        }

        private boolean compare(Object left, String operator, Object right) {
            if (isNumeric(left) && isNumeric(right)) {
                int compared = toBigDecimal(left).compareTo(toBigDecimal(right));
                return switch (operator) {
                    case ">" -> compared > 0;
                    case ">=" -> compared >= 0;
                    case "<" -> compared < 0;
                    case "<=" -> compared <= 0;
                    case "==", "=" -> compared == 0;
                    case "!=" -> compared != 0;
                    default -> false;
                };
            }
            int compared = String.valueOf(left).compareTo(String.valueOf(right));
            return switch (operator) {
                case "==", "=" -> Objects.equals(String.valueOf(left), String.valueOf(right));
                case "!=" -> !Objects.equals(String.valueOf(left), String.valueOf(right));
                case ">" -> compared > 0;
                case ">=" -> compared >= 0;
                case "<" -> compared < 0;
                case "<=" -> compared <= 0;
                default -> false;
            };
        }

        private boolean isNumeric(Object value) {
            if (value instanceof Number) {
                return true;
            }
            if (value == null) {
                return false;
            }
            try {
                new BigDecimal(String.valueOf(value).trim().replace(",", ""));
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        private boolean matchOperator(String operator) {
            return match(TokenType.OPERATOR, operator);
        }

        private boolean matchKeyword(String keyword) {
            if (peek(TokenType.IDENTIFIER, null)
                    && keyword.equals(tokens.get(index).text().toLowerCase(Locale.ROOT))) {
                index++;
                return true;
            }
            return false;
        }

        private void expectKeyword(String keyword) {
            if (!matchKeyword(keyword)) {
                throw new BusinessException("公式缺少关键字：" + keyword);
            }
        }

        private boolean match(TokenType type, String text) {
            if (!peek(type, text)) {
                return false;
            }
            index++;
            return true;
        }

        private void expect(TokenType type, String text) {
            if (!match(type, text)) {
                throw new BusinessException("公式语法错误，位置：" + index);
            }
        }

        private boolean peek(TokenType type, String text) {
            Token token = tokens.get(index);
            if (token.type() != type) {
                return false;
            }
            return text == null || text.equals(token.text());
        }

        private Token consume() {
            return tokens.get(index++);
        }
    }
}
