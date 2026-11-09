package com.mandiritransport.ops.finance.service;

import com.mandiritransport.ops.common.exception.BusinessException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

/**
 * Safe deterministic recursive-descent math expression evaluator.
 * Supports +, -, *, /, unary +/-, parentheses, numeric literals, and named variables.
 * Never uses ScriptEngine, eval, or reflection.
 */
public final class FormulaExpressionEvaluator {

    private static final MathContext MC = MathContext.DECIMAL64;

    private FormulaExpressionEvaluator() {
    }

    public static BigDecimal evaluate(String expression, Map<String, BigDecimal> variables) {
        if (expression == null || expression.isBlank()) {
            throw BusinessException.badRequest("INVALID_FORMULA_EXPRESSION", "Ekspresi formula tidak boleh kosong.");
        }
        Parser parser = new Parser(expression.trim(), variables != null ? variables : Map.of());
        BigDecimal result = parser.parseExpression();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw BusinessException.badRequest("INVALID_FORMULA_EXPRESSION",
                    "Karakter tidak terduga pada ekspresi formula di posisi " + parser.pos + ": '" + parser.peek() + "'");
        }
        return result;
    }

    private static final class Parser {
        private final String input;
        private final Map<String, BigDecimal> vars;
        private int pos = 0;

        private Parser(String input, Map<String, BigDecimal> vars) {
            this.input = input;
            this.vars = vars;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value = value.add(parseTerm(), MC);
                } else if (match('-')) {
                    value = value.subtract(parseTerm(), MC);
                } else {
                    break;
                }
            }
            return value;
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value = value.multiply(parseFactor(), MC);
                } else if (match('/')) {
                    BigDecimal divisor = parseFactor();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw BusinessException.badRequest("FORMULA_DIV_BY_ZERO", "Pembagian dengan nol pada formula biaya.");
                    }
                    value = value.divide(divisor, MC);
                } else {
                    break;
                }
            }
            return value;
        }

        private BigDecimal parseFactor() {
            skipWhitespace();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return parseFactor().negate(MC);
            }
            if (match('(')) {
                BigDecimal inner = parseExpression();
                skipWhitespace();
                if (!match(')')) {
                    throw BusinessException.badRequest("INVALID_FORMULA_EXPRESSION", "Kurung tutup ')' hilang pada formula.");
                }
                return inner;
            }
            if (isAtEnd()) {
                throw BusinessException.badRequest("INVALID_FORMULA_EXPRESSION", "Akhir ekspresi formula tidak lengkap.");
            }
            char ch = peek();
            if (Character.isDigit(ch) || ch == '.') {
                return parseNumber();
            }
            if (Character.isLetter(ch) || ch == '_') {
                return parseVariable();
            }
            throw BusinessException.badRequest("INVALID_FORMULA_EXPRESSION",
                    "Token tidak dikenali pada formula: '" + ch + "'");
        }

        private BigDecimal parseNumber() {
            int start = pos;
            boolean hasDot = false;
            while (!isAtEnd()) {
                char c = peek();
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' && !hasDot) {
                    hasDot = true;
                    pos++;
                } else {
                    break;
                }
            }
            String token = input.substring(start, pos);
            try {
                return new BigDecimal(token);
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest("INVALID_FORMULA_EXPRESSION", "Angka tidak valid pada formula: " + token);
            }
        }

        private BigDecimal parseVariable() {
            int start = pos;
            while (!isAtEnd()) {
                char c = peek();
                if (Character.isLetterOrDigit(c) || c == '_') {
                    pos++;
                } else {
                    break;
                }
            }
            String name = input.substring(start, pos);
            BigDecimal val = vars.get(name);
            if (val == null) {
                throw BusinessException.badRequest("UNKNOWN_FORMULA_VARIABLE",
                        "Variabel formula tidak ditemukan: '" + name + "'");
            }
            return val;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(peek())) {
                pos++;
            }
        }

        private boolean match(char expected) {
            if (!isAtEnd() && input.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        private char peek() {
            return input.charAt(pos);
        }

        private boolean isAtEnd() {
            return pos >= input.length();
        }
    }
}
