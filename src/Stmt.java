package com.craftinginterpreters.lox;

import java.util.List;

public interface Stmt {
  abstract <R> R accept(Visitor<R> visitor);

  interface Visitor<R> {
    R visitBlockStmt(Block expr);

    R visitExpressionStmt(Expression expr);

    R visitPrintStmt(Print expr);

    R visitVarStmt(Var expr);
  }

  record Block(List<Stmt> statements) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
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

  record Var(Token name, Expr initializer) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }
  }
}
