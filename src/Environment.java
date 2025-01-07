package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class Environment {
  private final Map<String, Object> values = new HashMap<>();
  final Environment enclosing;

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(String name, Optional<Object> value) {
    values.put(name, value.orElse(null));
  }

  boolean assign(String name, Object value) {
    if (values.containsKey(name)) {
      values.put(name, value);
      return true;
    }
    return Optional.ofNullable(enclosing).map(e -> e.assign(name, value)).orElse(false);
  }

  Optional<Object> get(String name) {
    return values.containsKey(name)
        ? Optional.ofNullable(values.get(name))
        : Optional.ofNullable(enclosing).flatMap(e -> e.get(name));
  }
}
