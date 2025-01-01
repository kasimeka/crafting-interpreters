package com.craftinginterpreters.lox;

public interface Stmt {
  abstract <R> R accept(Visitor<R> visitor);

  interface Visitor<R> {
    R visitExpressionStmt(Expression expr);

    R visitPrintStmt(Print expr);
  }

  record Expression(Expr expression) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

  record Print(Expr expression) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }
  }
}
