package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Optional;

public interface Stmt { // extends Grammar
  abstract <R> R accept(Visitor<R> visitor);

  interface Visitor<R> { // extends Grammar.Visitor<R>
    R visitBlockStmt(Block stmt);

    R visitBreakStmt(Break stmt);

    R visitExpressionStmt(Expression stmt);

    R visitFunctionStmt(Function stmt);

    R visitIfStmt(If stmt);

    R visitPrintStmt(Print stmt);

    R visitReturnStmt(Return stmt);

    R visitVarStmt(Var stmt);

    R visitWhileStmt(While stmt);
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

  record Expression(Expr expression) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

  record Function(Token name, Expr.Function definition) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
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

  record Return(Token keyword, Optional<Expr> value) implements Stmt {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }
  }

  record Var(Token name, Optional<Expr> initializer) implements Stmt {
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
