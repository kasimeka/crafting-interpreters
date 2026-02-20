package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private enum FunctionType {
    NONE,
    FUNCTION,
    METHOD
  }

  private final Interpreter interpreter;
  private final Stack<Map<String, Integer>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements());
    endScope();
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    define(stmt.name().lexeme());

    beginScope();
    define("this");
    for (var method : stmt.methods()) {
      resolveFunction(method.definition(), FunctionType.METHOD);
    }
    endScope();

    return null;
  }

  void resolve(List<Stmt> statements) {
    for (var statement : statements) {
      resolve(statement);
    }
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Integer>());
  }

  private void endScope() {
    final var scope = scopes.pop();
    scope.forEach(
        (k, v) -> {
          if (v <= 0) System.err.println("WARNING: unused variable `" + k + "`.");
        });
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object());
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    // declare(stmt.name());
    define(stmt.name().lexeme()); // the book hates this one weird trick!
    stmt.initializer().ifPresent(this::resolve);
    return null;
  }

  private void define(String name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name, 0);
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    /* the var x = x error goes here */

    resolveLocal(expr, expr.name().lexeme());
    return null;
  }

  private void resolveLocal(Expr expr, String name) {
    int hops = 0;
    for (var scope : scopes.reversed()) {
      final var refCount = scope.get(name);
      if (refCount != null) {
        interpreter.resolve(expr, hops);
        scope.put(name, refCount + 1);
        return;
      }
      hops++;
    }
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value());
    resolveLocal(expr, expr.name().lexeme());
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    define(stmt.name().lexeme());
    resolve(stmt.definition());
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break _stmt) {
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression());
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition());
    resolve(stmt.thenBranch());
    stmt.elseBranch().ifPresent(this::resolve);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression());
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword(), "return outside function.");
    }
    stmt.value().ifPresent(this::resolve);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition());
    resolve(stmt.body());
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left());
    resolve(expr.right());
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee());
    for (Expr argument : expr.arguments()) {
      resolve(argument);
    }
    return null;
  }

  @Override
  public Void visitFunctionExpr(Expr.Function expr) {
    resolveFunction(expr, FunctionType.FUNCTION);
    return null;
  }

  // used by function exprs, function stmts and methods
  private void resolveFunction(Expr.Function expr, FunctionType type) {
    final var enclosingFunction = currentFunction;
    currentFunction = type;
    beginScope();
    for (var param : expr.params()) {
      define(param.lexeme());
    }
    resolve(expr.body());
    endScope();
    currentFunction = enclosingFunction;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression());
    return null;
  }

  @Override
  public Void visitIfExpr(Expr.If expr) {
    resolve(expr.condition());
    resolve(expr.first());
    resolve(expr.second());
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal _expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left());
    resolve(expr.right());
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right());
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value());
    resolve(expr.object());
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    resolveLocal(expr, expr.keyword().lexeme());
    return null;
  }
}
