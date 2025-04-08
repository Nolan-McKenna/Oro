package oro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Oro {
  private static final Interpreter interpreter = new Interpreter();
  private static String gSource;
  static boolean hadError = false;
  static boolean hadRuntimeError = false;
  public static void main(String[] args) throws IOException {
    RegexFunctions.registerAll(interpreter);
    if (args.length > 1) {
      System.out.println("Usage: oro [script]");
      System.exit(64); 
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    // Indicate an error in the exit code.
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    System.out.println("REPL Started \n");
    for (;;) { 
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line);
      hadError = false;
    }
  }

  private static void run(String source) {
    gSource = source;
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();


    // Stop if there was a syntax error
    if (hadError) return;

    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);

    // Stop if there was a resolution error.
    if (hadError) return;
    
    interpreter.interpret(statements);
  }

  static void error(int line, int column, String message) {
    report(line, column, "", message);
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void runtimeError(RuntimeError error) {
    if (error.token != null){
      System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
    }
    else{
      System.err.println(error.getMessage());
    }
    
    hadRuntimeError = true;
  }

  private static void report(int line, int column, String where, String message) {
    String lineContent;
    String[] lines = gSource.split("\n");
    if (line - 1 >= lines.length){
      lineContent = lines[lines.length - 1];
      line = lines.length;
    }
    else{
      lineContent = lines[line - 1];
    }

    System.err.println("[line " + line + ", column " + column + "] Error" + where + ": " + message);
    System.err.println(line + " | " + lineContent);
    System.err.print(" ".repeat(column + (Integer.toString(line).length() + 3)));  // Position under the error
    System.err.println("^--");
    hadError = true;
  }

  private static void report(int line, String where, String message) {
    String lineContent;
    String[] lines = gSource.split("\n");
    if (line - 1 >= lines.length){
      lineContent = lines[lines.length - 1];
      line = lines.length;
    }
    else{
      lineContent = lines[line - 1];
    }

    System.err.println("[line " + line + "] Error" + where + ": " + message);
    System.err.println(line + " | " + lineContent);
    hadError = true;
  }


  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, token.column, " at end", message);
    } else {
      report(token.line, token.column, " at '" + token.lexeme + "'", message);
    }
  }
}