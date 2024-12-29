{
  lib,
  writeText,
  writeScriptBin,
  google-java-format,
  exprs ? {
    Binary = "Expr left, Token operator, Expr right";
    Unary = "Token operator, Expr right";
    Grouping = "Expr expression";
    Literal = "Object value";
    Ternary = "Expr condition, Expr first, Expr second";
  },
}:
writeScriptBin "generate-expr-file" ''
  #!/usr/bin/env bash
  ${lib.getExe google-java-format} \
  ${writeText "Expr.java" (lib.concatStringsSep "\n" [
    ''
      package com.craftinginterpreters.lox;

      public interface Expr {
        abstract <R> R accept(Visitor<R> visitor);

        interface Visitor<R> {''
    (lib.concatMapAttrsStringSep "\n" (baseName: _fields: ''
        R visit${baseName}Expr(${baseName} expr);
      '')
      exprs)
    "}"
    (lib.concatMapAttrsStringSep "\n" (baseName: fields: ''
        record ${baseName}(${fields}) implements Expr {
          @Override
          public <R> R accept(Visitor<R> visitor) {
            return visitor.visit${baseName}Expr(this);
          }
        }
      '')
      exprs)
    "}"
  ])}
''
