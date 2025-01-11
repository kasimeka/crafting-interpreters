package com.craftinginterpreters.lox;

import static janw4ld.utils.Utils.supply;

import java.util.List;
import java.util.Optional;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  boolean isRepl;
  private final AstPrinter printer = new AstPrinter();
  private Environment environment = new Environment();
  private boolean unwindingLoop = false;

  Interpreter() {
    this.isRepl = false;
  }

  Interpreter(boolean isRepl) {
    this.isRepl = isRepl;
  }

  class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Expr expr, Token token, String message) {
      super(Optional.ofNullable(expr).map(e -> printer.print(e) + ": ").orElse("") + message);
      this.token = token;
    }
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) execute(statement);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
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
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    final var e = stmt.expression();
    if (isRepl) {
      execute(new Stmt.Print(e));
    } else {
      evaluate(e);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    System.out.println(stringify(evaluate(stmt.expression())));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    environment.define(
        stmt.name().lexeme(), Optional.ofNullable(stmt.initializer()).map(this::evaluate));
    return null;
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
  public Object visitLogicalExpr(Expr.Logical expr) {
    final var left = evaluate(expr.left());
    switch (expr.operator().kind()) {
      case TokenKind.OR -> {
        if (isTruthy(left)) return left;
      }
      case TokenKind.AND -> {
        if (!isTruthy(left)) return left;
      }
    }
    return evaluate(expr.right());
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    final var operator = expr.operator();
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
  public Object visitIfExpr(Expr.If expr) {
    return isTruthy(evaluate(expr.condition())) ? evaluate(expr.first()) : evaluate(expr.second());
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    final var name = expr.name();
    final var key = name.lexeme();
    return environment
        .get(key)
        .orElseThrow(
            supply(new RuntimeError(expr, name, "Variable `" + key + "` used before assignment")));
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    final var name = expr.name();
    final var key = name.lexeme();
    final var value = evaluate(expr.value()); // ! side effects always trigger
    if (!environment.assign(key, value))
      throw new RuntimeError(expr, name, "Undefined variable `" + key + "`.");
    return value;
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

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition()))) {
      execute(stmt.body());
      if (unwindingLoop) return null;
    }
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    if (!stmt.enclosedInLoop()) unwindingLoop = false;
    if (unwindingLoop) return null;
    executeBlock(stmt.statements(), new Environment(environment));
    return null;
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    final var previous = this.environment;
    try {
      this.environment = environment;
      for (Stmt statement : statements) {
        if (unwindingLoop) break;
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition()))) {
      execute(stmt.thenBranch());
    } else {
      stmt.elseBranch().ifPresent(this::execute);
    }
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    unwindingLoop = true;
    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue expr) {
    return null; // TODO:
  }
}
