package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {
  private static final AstPrinter printer = new AstPrinter();

  class RuntimeError extends RuntimeException {
    final Token token;
    final Expr expr;

    RuntimeError(Expr expr, Token token, String message) {
      super(printer.print(expr) + ": " + message);
      this.expr = expr;
      this.token = token;
    }
  }

  String interpret(Expr expression) {
    try {
      final var value = evaluate(expression);
      return stringify(value);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
      return "";
    }
  }

  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value();
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression());
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    final var operator = expr.operator();
    final var operand = evaluate(expr.right());
    final var k = operator.kind();
    return switch (k) {
      case MINUS -> {
        checkOperands(Number.class, expr, operator, operand);
        yield -(double) operand;
      }
      case BANG -> {
        checkOperands(Boolean.class, expr, operator, operand);
        yield !isTruthy(operand);
      }
      default -> {
        throw new RuntimeError(expr, operator, "unimplemented unary operator " + k.toString());
      }
    };
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    final var operator = expr.operator();
    // can't short circuit boolean operators
    final var left = evaluate(expr.left());
    final var right = evaluate(expr.right());

    final var k = operator.kind();
    try {
      return switch (k) {
        case PLUS -> {
          if (left instanceof Double && right instanceof Double) {
            yield (double) left + (double) right;
          }
          if (left instanceof String && right instanceof String) {
            yield (String) left + (String) right;
          }
          throw new RuntimeError(expr, operator, "type mismatch between operands");
        }
        case MINUS -> (double) left - (double) right;
        // if (right.equals(0.0)) throw new RuntimeError(expr, operator, "devision by zero :(");
        case SLASH -> (double) left / (double) right; // div by zero returns Infinity
        case STAR -> (double) left * (double) right;
        case GREATER -> (double) left > (double) right;
        case GREATER_EQUAL -> (double) left >= (double) right;
        case LESS -> (double) left < (double) right;
        case LESS_EQUAL -> (double) left <= (double) right;
        case BANG_EQUAL -> !isEqual(left, right);
        case EQUAL_EQUAL -> isEqual(left, right);
        default -> {
          throw new RuntimeError(expr, operator, "unimplemented binary operator " + k.toString());
        }
      };
    } catch (ClassCastException _exc) {
      checkOperands(Number.class, expr, operator, right);
      throw new RuntimeException("unreachable");
    }
  }

  @Override
  public Object visitTernaryExpr(Expr.Ternary expr) {
    return isTruthy(expr.condition()) ? evaluate(expr.first()) : evaluate(expr.second());
  }

  private boolean isTruthy(Object value) {
    if (value == null) return false; // null is a bitch in java :(
    // the book only considers nil and false as falsey
    return !(value.equals(false) || value.equals(0.0));
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null || right == null) return false;
    return left.equals(right); // this mean NaN equals NaN
  }

  private void checkOperands(Class<?> type, Expr expr, Token operator, Object... operands) {
    for (Object operand : operands) {
      if (!type.isInstance(operand)) {
        throw new RuntimeError(
            expr, operator, "Operands must be " + type.getSimpleName().toLowerCase() + "s.");
      }
    }
  }
}
