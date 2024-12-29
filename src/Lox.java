package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {
  static boolean hadError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
      System.out.println("\ngoodbye fren :)");
    }
  }

  private static void runFile(String path) throws IOException {
    final var bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError) System.exit(65);
  }

  private static void runPrompt() throws IOException {
    final var input = new InputStreamReader(System.in);
    final var reader = new BufferedReader(input);

    while (true) {
      System.out.print("> ");
      final var line = reader.readLine();
      if (line == null) break;
      run(line);
      hadError = false;
    }
  }

  private static void run(String source) {
    final var scanner = new Scanner(source);
    final var tokens = scanner.scanTokens();

    final var parser = new Parser(tokens);
    final var expr = parser.parse();
    if (hadError) return;

    System.out.println(new AstPrinter().print(expr));
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void error(Token token, String message) {
    if (token.kind() == TokenKind.EOF) {
      report(token.line(), " at end", message);
    } else {
      report(token.line(), " at '" + token.lexeme() + "'", message);
    }
  }

  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
}
