package com.craftinginterpreters.lox;

import java.util.Optional;

record Token(TokenKind kind, String lexeme, Optional<Object> literal, int line) {
  public String toString() {
    return line + ": " + kind + " :: " + lexeme + literal.map(l -> (" : " + l)).orElse("");
  }
}
