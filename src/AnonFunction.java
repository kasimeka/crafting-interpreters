package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Optional;

class AnonFunction implements LoxCallable {
  private final Expr.Function definition;
  private final Environment closure;

  AnonFunction(Expr.Function definition, Environment closure) {
    this.definition = definition;
    this.closure = closure;
  }

  public Object call(Interpreter interpreter, List<Object> arguments) {
    final var environment = new Environment(closure);
    final var params = definition.params();
    for (int i = 0; i < params.size(); i++) {
      environment.define(params.get(i).lexeme(), Optional.of(arguments.get(i)));
    }
    try {
      interpreter.executeBlock(definition.body().statements(), environment);
    } catch (Interpreter.Return exc) {
      return exc.value.orElse(null);
    }
    return null;
  }

  public int arity() {
    return definition.params().size();
  }

  public String toString() {
    return "<anonymous fn " + Integer.toHexString(definition.hashCode()) + ">";
  }
}
