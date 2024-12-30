package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {
  private static final AstPrinter printer = new AstPrinter();

  class RuntimeError extends RuntimeException {
    final Token token;
    final Expr expr;

    RuntimeError(Expr expr, Token token, String message) {
      super(message);
      this.token = token;
      this.expr = expr;
    }
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value();
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression());
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    final var operator = expr.operator();
    final var right = evaluate(expr.right());
    final var k = operator.kind();
    return switch (k) {
      case MINUS -> {
        if (!(right instanceof Double))
          throw new RuntimeError(expr, operator, "Operand must be a number.");
        yield -(double) right;
      }
      case BANG -> {
        if (!(right instanceof Boolean))
          throw new RuntimeError(expr, operator, "Operand must be a boolean.");
        yield !isTruthy(right);
      }
      default ->
          throw new RuntimeError(expr, operator, "unimplemented unary operator " + k.toString());
    };
  }

  private boolean checkNumberOperand(Token operator, Object operand) {
    return (operand instanceof Double);
    // throw new RuntimeError(operator, "Operand must be a number.");
  }

  private boolean isTruthy(Object value) {
    if (value == null) return false; // null is a bitch in java :(
    // the book only considers nil and false as falsey
    return !(value.equals(false) || value.equals(0.0));
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    final var operator = expr.operator();
    // can't short circuit boolean operators yet
    final var left = evaluate(expr.left());
    final var right = evaluate(expr.right());

    final var k = operator.kind();
    return switch (k) {
      case MINUS -> (double) left - (double) right;
      case SLASH -> (double) left / (double) right;
      case PLUS -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double) left + (double) right;
        }
        if (left instanceof String && right instanceof String) {
          yield (String) left + (String) right;
        }
        throw new RuntimeError(expr, operator, "type mismatch in the shallowest (+)");
      }
      case STAR -> (double) left * (double) right;
      case GREATER -> (double) left > (double) right;
      case GREATER_EQUAL -> (double) left >= (double) right;
      case LESS -> (double) left < (double) right;
      case LESS_EQUAL -> (double) left <= (double) right;
      case BANG_EQUAL -> !isEqual(left, right);
      case EQUAL_EQUAL -> isEqual(left, right);

      default ->
          throw new RuntimeError(expr, operator, "unimplemented unary operator " + k.toString());
    };
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null || right == null) return false;
    return left.equals(right); // this mean NaN equals NaN
  }

  @Override
  public Object visitTernaryExpr(Expr.Ternary expr) {
    // TODO
    return null;
  }
}
