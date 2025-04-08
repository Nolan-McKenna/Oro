package oro;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static oro.TokenType.*; 

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private int column = 0;
  private int prevCol = 0;

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    AND);
    keywords.put("class",  CLASS);
    keywords.put("else",   ELSE);
    keywords.put("false",  FALSE);
    keywords.put("for",    FOR);
    keywords.put("fun",    FUN);
    keywords.put("if",     IF);
    keywords.put("null",    NULL);
    keywords.put("or",     OR);
    keywords.put("print",  PRINT);
    keywords.put("return", RETURN);
    keywords.put("super",  SUPER);
    keywords.put("self",   SELF);
    keywords.put("true",   TRUE);
    keywords.put("def",    DEF);
    keywords.put("then",  THEN);
    keywords.put("import",  IMPORT);
    keywords.put("as",  AS);
    keywords.put("in",  IN);
    keywords.put("break",  BREAK);
    keywords.put("continue",  CONTINUE);
    keywords.put("extends", EXTENDS);
  }

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line, column));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case '[': addToken(LEFT_BRACKET); break;
      case ']': addToken(RIGHT_BRACKET); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break; 
      case '/': addToken(SLASH); break;
      case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
      case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
      case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
      case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
      // For comment, end token at end of line
      case '#': while (peek() != '\n' && !isAtEnd()) advance(); break;
      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break; 
      case '\n': 
        line++; 
        if (column > 0){
          prevCol = column; 
          column = 0;
        }
        
        break;
      case 'f':
            if (match('"')) {
                fString();
            } 
            break;
      case '"': string(c); break;
      case '\'': string(c); break;

      default:
        if (isDigit(c)) {
            number();
        } else if (isAlpha(c)) {
            identifier();
        } else { 
          if (column == 0){
            Oro.error(line, prevCol, "Unexpected character.");
          }
          else{
            Oro.error(line, column, "Unexpected character.");
          }
        }
      break;
    }
  }

  private void identifier() { 
    while (isAlphaNumeric(peek())) advance();
    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }

  private void number() {
    // Consume up to the "."
    while (isDigit(peek())) advance();

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      // Consume all digits after the "."
      while (isDigit(peek())) advance();
    }

    addToken(NUMBER,
        Double.parseDouble(source.substring(start, current)));
  }

  private void string(char quoteType) {
    while (peek() != quoteType && !isAtEnd()) {
      if (peek() == '\n'){
        line++;
        if (column > 0){
          prevCol = column; 
          column = 0;
        }
      }
      advance();
    }

    if (isAtEnd()) {
      Oro.error(line, "Unterminated string.");
      return;
    }

    advance();

    // Trim quotes
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  private void fString() {
    int depth = 0;  
    int checkPos = current;
    
    while (checkPos < source.length()) {
        char c = source.charAt(checkPos);
        
        if (c == '{') {
            depth++;
        } else if (c == '}') {
            depth--;
            if (depth < 0) {
                Oro.error(line, "Unmatched closing brace in f-string.");
                return;
            }
        } else if (c == '"' && source.charAt(checkPos - 1) != '\\') {
            if (depth > 0) {
                Oro.error(line, "Unclosed expression in f-string.");
                return;
            }
            break;
        }
        checkPos++;
    }
    
    if (checkPos >= source.length()) {
      if (column == 0){
        Oro.error(line, prevCol, "Unterminated f-string.");
      }
      else{
        Oro.error(line, column, "Unterminated f-string.");
      }
      return;
    }
    
    // If validated, process f-string
    while (!isAtEnd()) {
        if (peek() == '"') {
            advance();  // Consume closing quote
            String value = source.substring(start + 2, current - 1); // Exclude `f"` and `"`
            addToken(TokenType.FSTRING, value);
            return;
        }
        advance();
    }
    
    if (column == 0){
      Oro.error(line, prevCol, "Unterminated f-string.");
    }
    else{
      Oro.error(line, column, "Unterminated f-string.");
    }
}


  // Conditional advance
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  // Helper Methods
  // One char lookahead
  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  // Two char lookahead for fractional number literals
  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  } 

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  } 

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private char advance() {
    column++;
    return source.charAt(current++);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line, column));
  }
}
