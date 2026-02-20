package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class LoxClass implements LoxCallable {
  final String name;
  private final Map<String, LoxFunction> methods;

  LoxClass(String name, Map<String, LoxFunction> methods) {
    this.name = name;
    this.methods = methods;
  }

  LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }

    return null;
  }

  @Override
  public String toString() {
    return "<class " + name + ">";
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    return new Instance(this);
  }

  @Override
  public int arity() {
    return 0;
  }

  class Instance {
    private final LoxClass classDefinition;
    private final Map<String, Object> fields = new HashMap<>();

    Instance(LoxClass classDefinition) {
      this.classDefinition = classDefinition;
    }

    Object get(Interpreter interpreter, Token name) {
      final var key = name.lexeme();
      // if (fields.containsKey(key)) { return fields.get(key); }
      return Optional.ofNullable(fields.get(key))
          .or(() -> Optional.ofNullable(classDefinition.findMethod(key)).map(m -> m.bind(this)))
          .orElseThrow(
              () -> interpreter.new RuntimeError(name, "Undefined property `" + key + "`."));
    }

    void set(String name, Object value) {
      fields.put(name, value);
    }

    @Override
    public String toString() {
      return "<class instance "
          + classDefinition.name
          + ":"
          + Integer.toHexString(this.hashCode())
          + ">";
    }
  }
}
