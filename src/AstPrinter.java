package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Optional;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  int depth = 0;

  String print(Expr expr) {
    return expr.accept(this);
  }

  String print(Stmt stmt) {
    return stmt.accept(this);
  }

  String print(List<Stmt> stmts) {
    final var shouldNest = stmts.size() != 1;
    if (shouldNest) depth += 1;
    final var in = "  ".repeat(depth);
    final var result =
        !shouldNest
            ? print(stmts.getLast())
            : "(do\n" + in + String.join("\n" + in, stmts.stream().map(this::print).toList()) + ")";
    if (shouldNest) depth -= 1;
    return result;
  }

  private String renderTree(String name, Expr... exprs) {
    final var builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ").append(print(expr));
    }
    builder.append(")");

    return builder.toString();
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return renderTree(expr.operator().lexeme(), expr.left(), expr.right());
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return renderTree("group", expr.expression());
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    final var v = expr.value();
    if (v == null) return "nil";
    return "«" + v.toString() + "»";
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return renderTree(expr.operator().lexeme(), expr.right());
  }

  @Override
  public String visitTernaryExpr(Expr.Ternary expr) {
    return renderTree("if", expr.condition(), expr.first(), expr.second());
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name().lexeme();
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return renderTree("assign '" + expr.name().lexeme(), expr.value());
  }

  @Override
  public String visitExpressionStmt(Stmt.Expression stmt) {
    return print(stmt.expression());
  }

  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return renderTree("print", stmt.expression());
  }

  @Override
  public String visitVarStmt(Stmt.Var stmt) {
    final var decl = "declare '" + stmt.name().lexeme();
    return Optional.ofNullable(stmt.initializer())
        .map(i -> renderTree(decl, i))
        .orElse(renderTree(decl));
  }

  @Override
  public String visitBlockStmt(Stmt.Block stmt) {
    return print(stmt.statements());
  }
}
