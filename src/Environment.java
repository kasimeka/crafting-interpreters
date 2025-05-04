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

  void assignAt(int distance, String name, Object value) {
    ancestor(distance).values.put(name, value);
  }

  Optional<Object> get(String name) {
    return values.containsKey(name)
        ? Optional.ofNullable(values.get(name))
        : Optional.ofNullable(enclosing).flatMap(e -> e.get(name));
  }

  Optional<Object> getAt(int distance, String name) {
    return Optional.ofNullable(
        Optional.ofNullable(ancestor(distance))
            .orElseThrow(() -> new RuntimeException("unreachable"))
            .values
            .get(name));
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    while (distance-- > 0) {
      environment = environment.enclosing;
    }
    return environment;
  }
}
