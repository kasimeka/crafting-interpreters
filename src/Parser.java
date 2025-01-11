package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenKind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class Parser {
  static final class ParseError extends RuntimeException {
    final Token token;

    ParseError(Token token) {
      this.token = token;
    }
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    final var statements = new ArrayList<Stmt>();
    while (!atEof()) {
      statements.add(declaration());
    }
    return statements;
  }

  private Stmt declaration() {
    try {
      return tryConsume(VAR) ? varDeclaration() : statement();
    } catch (ParseError _error) {
      // TODO: add Stmt subclass to represent parse errors
      recover();
      return new Stmt.Expression(new Expr.Literal(ERROR));
    }
  }

  private Stmt varDeclaration() {
    mustConsume(IDENTIFIER, "Expected variable name.");
    final var name = current();
    final var initializer = tryConsume(EQUAL) ? ifExpression() : null;
    mustConsume(SEMICOLON, "Expected ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt statement() {
    if (tryConsume(FOR)) return forStatement();
    if (tryConsume(IF)) return ifStatement();
    if (tryConsume(WHILE)) return whileStatement();
    if (tryConsume(PRINT)) return printStatement();
    if (nextIs(LEFT_BRACE)) return block("impossible :)");
    return expressionStatement();
  }

  private Stmt forStatement() {
    // mustConsume(LEFT_PAREN, "Expected '(' after `for`.");
    final var initializer =
        tryConsume(SEMICOLON)
            ? Optional.<Stmt>empty()
            : tryConsume(VAR) ? Optional.of(varDeclaration()) : Optional.of(expressionStatement());

    final var condition = nextIs(SEMICOLON) ? new Expr.Literal(true) : expression();
    mustConsume(SEMICOLON, "Expected ';' after loop condition.");

    final var increment = nextIs(LEFT_BRACE) ? Optional.<Expr>empty() : Optional.of(expression());
    // mustConsume(RIGHT_PAREN, "Expect ')' after for clauses.");

    final var body = block("Expected '{' after for clauses.").statements();
    increment.ifPresent(e -> body.add(new Stmt.Expression(e)));

    final var iterations = new Stmt.While(condition, new Stmt.Block(body));
    return initializer
        .<Stmt>map(init -> new Stmt.Block(Arrays.asList(init, iterations)))
        .orElse(iterations);
  }

  private Stmt whileStatement() {
    return new Stmt.While(expression(), block("Expected '{' after while condition."));
  }

  private Stmt ifStatement() {
    final var condition = expression();

    final var thenBranch = block("Expected '{' after if condition.");

    var elseBranch = Optional.<Stmt.Block>empty();
    if (tryConsume(ELSE)) {
      elseBranch = Optional.of(block("Expected '{' after `else`."));
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    final var value = ifExpression();
    mustConsume(SEMICOLON, "Expected ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt expressionStatement() {
    final var expr = ifExpression();
    mustConsume(SEMICOLON, "Expected ';' after expression.");
    return new Stmt.Expression(expr);
  }

  private Stmt.Block block(String errMsg) {
    mustConsume(LEFT_BRACE, errMsg);
    final var statements = new ArrayList<Stmt>();
    while (!nextIs(RIGHT_BRACE) && !atEof()) {
      statements.add(declaration()); // drop errors
    }
    mustConsume(RIGHT_BRACE, "Expected '}' after block.");
    return new Stmt.Block(statements);
  }

  private Expr ifExpression() {
    if (!nextIs(IFX)) return sequence();

    final var conditionals = new ArrayList<Expr>();
    while (tryConsume(IFX)) {
      conditionals.add(sequence());
      // mustConsume(THEN, "Expected `then` after ternary condition");
    }
    var expr = sequence();
    for (var c : conditionals.reversed()) {
      // mustConsume(ELSE, "Expected `else` after ternary condition");
      final var second = sequence();
      expr = new Expr.If(c, expr, second);
    }
    return expr;
  }

  private Expr sequence() {
    var expr = expression();
    while (tryConsume(COMMA)) {
      final var operator = current();
      final var right = expression();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    final var expr = or();
    if (tryConsume(EQUAL)) {
      final var equals = current();
      final var value = assignment();
      if (expr instanceof Expr.Variable) {
        final var name = ((Expr.Variable) expr).name();
        return new Expr.Assign(name, value);
      }
      error(equals, "Invalid assignment target.");
    }
    return expr;
  }

  private Expr or() {
    var expr = and();
    while (tryConsume(OR)) {
      final var operator = current();
      final var right = and();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr and() {
    var expr = equality();
    while (tryConsume(AND)) {
      final var operator = current();
      final var right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr equality() {
    var expr = comparison();
    while (tryConsume(BANG_EQUAL, EQUAL_EQUAL)) {
      final var operator = current();
      final var right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr comparison() {
    var expr = term();
    while (tryConsume(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      final var operator = current();
      final var right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr term() {
    var expr = factor();
    while (tryConsume(MINUS, PLUS)) {
      final var operator = current();
      final var right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr factor() {
    var expr = unary();
    while (tryConsume(SLASH, STAR)) {
      final var operator = current();
      final var right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr unary() {
    if (tryConsume(BANG, MINUS)) {
      final var operator = current();
      final var right = unary();
      return new Expr.Unary(operator, right);
    }
    return primary();
  }

  private Expr primary() {
    advance();
    return switch (current().kind()) {
      case FALSE -> new Expr.Literal(false);
      case TRUE -> new Expr.Literal(true);
      case NIL -> new Expr.Literal(null);
      case NUMBER, STRING -> new Expr.Literal(current().literal().get());
      case LEFT_PAREN -> {
        final var e = sequence();
        mustConsume(RIGHT_PAREN, "Expected ')' after expression.");
        yield new Expr.Grouping(e);
      }
      case IDENTIFIER -> new Expr.Variable(current());
      default -> throw error(current(), "Expected expression.");
    };
  }

  private void recover() {
    while (!atEof()) {
      if (current().kind() == SEMICOLON) return;
      switch (peek().kind()) {
        case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN, LEFT_BRACE -> {
          return;
        }
        default -> {
          advance();
        }
      }
    }
  }

  private boolean tryConsume(TokenKind... kinds) {
    for (final var k : kinds) {
      if (nextIs(k)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private boolean mustConsume(TokenKind kind, String message) {
    if (nextIs(kind)) {
      advance();
      return true;
    }
    throw error(peek(), message);
  }

  private ParseError error(Token t, String message) {
    Lox.error(t, message);
    return new ParseError(new Token(ERROR, t.lexeme(), Optional.of(message), t.line()));
  }

  private boolean nextIs(TokenKind expected) {
    if (atEof()) return false;
    return peek().kind() == expected;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token current() {
    return tokens.get(current - 1);
  }

  private void advance() {
    if (!atEof()) current += 1;
    // return previous();
  }

  private boolean atEof() {
    return peek().kind() == EOF;
  }
}
