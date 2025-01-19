package com.craftinginterpreters.lox;

class LoxFunction extends AnonFunction {
  private final Token name;
  private final int hash;

  LoxFunction(Stmt.Function decl, Environment closure) {
    super(decl.definition(), closure);
    this.name = decl.name();
    this.hash = decl.definition().hashCode();
  }

  @Override
  public String toString() {
    return "<fn " + name.lexeme() + ", " + Integer.toHexString(hash) + ">";
  }
}
