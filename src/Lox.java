package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
      System.out.println("\ngoodbye :)");
    }
  }

  private static void runPrompt() throws IOException {
    final var input = new InputStreamReader(System.in);
    final var reader = new BufferedReader(input);
    final var printer = new AstPrinter();
    final var repl = new Interpreter(true); // isRepl

    while (true) {
      System.out.print(">>> ");
      var line = reader.readLine();
      if (line == null) break;

      final var scanner = new Scanner(line);
      final var tokens = scanner.scanTokens();

      final var parser = new Parser(tokens);
      final var stmts = parser.parse();
      System.out.println(printer.print(stmts));
      if (!hadError) repl.interpret(stmts);
      hadError = false;
      hadRuntimeError = false;
    }
  }

  private static void runFile(String path) throws IOException {
    final var bytes = Files.readAllBytes(Paths.get(path));

    final var scanner = new Scanner(new String(bytes, Charset.defaultCharset()));
    final var tokens = scanner.scanTokens();

    final var parser = new Parser(tokens);
    final var stmts = parser.parse(); // we're doing only one expression
    if (hadError) System.exit(65);

    (new Interpreter()).interpret(stmts);
    if (hadRuntimeError) System.exit(70);
  }

  static void runtimeError(Interpreter.RuntimeError error) {
    hadRuntimeError = true;
    error(error.token.line(), error.getMessage());
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void error(Token token, String message) {
    if (token.kind() == TokenKind.EOF) {
      report(token.line(), " at end", message);
    } else {
      report(token.line(), " at `" + token.lexeme() + "`", message);
    }
  }

  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
}
