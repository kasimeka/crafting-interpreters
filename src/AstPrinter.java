package com.craftinginterpreters.lox;

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  private String renderTree(String name, Expr... exprs) {
    final var builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ").append(expr.accept(this));
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
}
