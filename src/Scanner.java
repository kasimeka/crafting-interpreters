package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenKind.*;
import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!atEof()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }
    tokens.add(new Token(EOF, "", Optional.empty(), line));
    return tokens;
  }

  private void scanToken() {
    final var c = advance();
    switch (c) {
      case '(' -> addToken(LEFT_PAREN);
      case ')' -> addToken(RIGHT_PAREN);
      case '{' -> addToken(LEFT_BRACE);
      case '}' -> addToken(RIGHT_BRACE);
      case ';' -> addToken(SEMICOLON);
      case ',' -> addToken(COMMA);
      case '.' -> addToken(DOT);
      case '-' -> addToken(MINUS);
      case '+' -> addToken(PLUS);
      case '*' -> addToken(STAR);
      // case '?' -> addToken(QUESTION_MARK);
      // case ':' -> addToken(COLON);
      case '!' -> addToken(consumeChar('=') ? BANG_EQUAL : BANG);
      case '=' -> addToken(consumeChar('=') ? EQUAL_EQUAL : EQUAL);
      case '<' -> addToken(consumeChar('=') ? LESS_EQUAL : LESS);
      case '>' -> addToken(consumeChar('=') ? GREATER_EQUAL : GREATER);
      case '/' -> {
        if (consumeChar('/')) {
          while (peek() != '\n' && !atEof()) advance();
        } else {
          addToken(SLASH);
        }
      }
      case ' ', '\r', '\t' -> {}
      case '\n' -> {
        line += 1;
      }
      case '"' -> {
        string();
      }
      default -> {
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Lox.error(line, "Unexpected character `" + c + "`.");
        }
      }
    }
  }

  private void string() {
    while (peek() != '"' && !atEof()) {
      if (peek() == '\n') line += 1;
      advance();
    }
    if (atEof()) {
      Lox.error(line, "Unterminated string.");
      return;
    }
    // consume the closing ".
    advance();

    // trim the surrounding quotes.
    final var value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  private void number() {
    while (isDigit(peek())) advance();

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();

    final var text = source.substring(start, current);
    final var type = keywords.getOrDefault(text, IDENTIFIER);

    addToken(type);
  }

  private boolean atEof() {
    return current >= source.length();
  }

  private char advance() {
    return source.charAt(current++);
  }

  private char peek() {
    if (atEof()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private void addToken(TokenKind type) {
    addToken(type, null);
  }

  private void addToken(TokenKind type, Object literal) {
    final var text = source.substring(start, current);
    tokens.add(new Token(type, text, Optional.ofNullable(literal), line));
  }

  private boolean consumeChar(char expected) {
    if (peek() != expected) return false;
    current += 1;
    return true;
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private static final Map<String, TokenKind> keywords =
      Map.ofEntries(
          entry("and", AND),
          entry("break", BREAK),
          entry("class", CLASS),
          entry("continue", CONTINUE),
          entry("else", ELSE),
          entry("false", FALSE),
          entry("for", FOR),
          entry("fun", FUN),
          entry("if", IF),
          entry("ifx", IFX),
          entry("nil", NIL),
          entry("or", OR),
          entry("print", PRINT),
          entry("return", RETURN),
          entry("super", SUPER),
          // entry("then", THEN),
          entry("this", THIS),
          entry("true", TRUE),
          entry("var", VAR),
          entry("while", WHILE));
}
