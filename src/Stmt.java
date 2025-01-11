package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Optional;

public interface Stmt { // extends Grammar
  abstract <R> R accept(Visitor<R> visitor);

  interface Visitor<R> { // extends Grammar.Visitor<R>
    R visitBlockStmt(Block expr);

    R visitBreakStmt(Break expr);

    R visitContinueStmt(Continue expr);

    R visitExpressionStmt(Expression expr);

    R visitIfStmt(If expr);

    R visitPrintStmt(Print expr);

    R visitVarStmt(Var expr);

    R visitWhileStmt(While expr);
  }

  record Block(List<Stmt> statements, boolean enclosedInLoop) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
  }

  record Break() implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }
  }

  record Continue() implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }
  }

  record Expression(Expr expression) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

  record If(Expr condition, Stmt.Block thenBranch, Optional<Stmt.Block> elseBranch)
      implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
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

  record While(Expr condition, Stmt.Block body) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }
  }
}
