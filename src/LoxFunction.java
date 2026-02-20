package com.craftinginterpreters.lox;

import java.util.Optional;

class LoxFunction extends AnonFunction {
  private final Token name;
  private final int hash;

  LoxFunction(Stmt.Function decl, Environment closure) {
    super(decl.definition(), closure);
    this.name = decl.name();
    this.hash = decl.definition().hashCode();
  }

  LoxFunction bind(LoxClass.Instance instance) {
    final var environment = new Environment(closure);
    environment.define("this", Optional.of(instance));
    return new LoxFunction(new Stmt.Function(name, definition), environment);
  }

  @Override
  public String toString() {
    return "<fn " + name.lexeme() + ", " + Integer.toHexString(hash) + ">";
  }
}
