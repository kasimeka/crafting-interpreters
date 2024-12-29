package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenKind.*;

import java.util.ArrayList;
import java.util.List;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return ternary();
    } catch (ParseError _error) {
      return null;
    }
  }

  private Expr ternary() {
    final var conditionals = new ArrayList<Expr>();
    conditionals.add(sequence());
    while (consumeOneOf(QUESTION_MARK)) {
      conditionals.add(sequence());
    }

    // [1 ? 2 ?] 3 [: 4 : 5]
    var expr = conditionals.removeLast(); // [?] 3 [:]
    for (var c : conditionals.reversed()) {
      mustConsume(COLON, "Expected ':' in ternary expression.");
      final var second = sequence();
      expr = new Expr.Ternary(c, expr, second);
    }
    return expr;
  }

  private Expr sequence() {
    var expr = expression();
    while (consumeOneOf(COMMA)) {
      final var operator = previous();
      final var right = expression();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr expression() {
    return equality();
  }

  private Expr equality() {
    var expr = comparison();
    while (consumeOneOf(BANG_EQUAL, EQUAL_EQUAL)) {
      final var operator = previous();
      final var right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr comparison() {
    var expr = term();
    while (consumeOneOf(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      final var operator = previous();
      final var right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr term() {
    var expr = factor();
    while (consumeOneOf(MINUS, PLUS)) {
      final var operator = previous();
      final var right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr factor() {
    var expr = unary();
    while (consumeOneOf(SLASH, STAR)) {
      final var operator = previous();
      final var right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr unary() {
    if (consumeOneOf(BANG, MINUS)) {
      final var operator = previous();
      final var right = unary();
      return new Expr.Unary(operator, right);
    }
    return primary();
  }

  private Expr primary() {
    return switch (advance().kind()) {
      case FALSE -> new Expr.Literal(false);
      case TRUE -> new Expr.Literal(true);
      case NIL -> new Expr.Literal(null);
      case NUMBER, STRING -> new Expr.Literal(previous().literal().get());
      case LEFT_PAREN -> {
        final var e = sequence();
        mustConsume(RIGHT_PAREN, "Expected ')' after expression.");

        yield new Expr.Grouping(e);
      }
      default -> throw error(previous(), "Expected expression.");
    };
  }

  private void synchronize() {
    advance();
    while (!atEof()) {
      if (previous().kind() == SEMICOLON) return;
      switch (peek().kind()) {
        case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
          return;
        }
        default -> {
          advance();
        }
      }
    }
  }

  private boolean consumeOneOf(TokenKind... kinds) {
    for (final var k : kinds) {
      if (check(k)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token mustConsume(TokenKind kind, String message) {
    if (check(kind)) return advance();
    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private boolean check(TokenKind expected) {
    if (atEof()) return false;
    return peek().kind() == expected;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token advance() {
    if (!atEof()) current += 1;
    return previous();
  }

  private boolean atEof() {
    return peek().kind() == EOF;
  }
}
