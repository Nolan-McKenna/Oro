package oro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONParser {
    private String json;
    private int current;

    public JSONParser(String json) {
        this.json = json;
        this.current = 0;
    }

    public Object parse() {
        skipWhitespace();
        char c = peek();
        
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
                return parseTrue();
            case 'f':
                return parseFalse();
            case 'n':
                return parseNull();
            default:
                return parseNumber();
        }
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> object = new LinkedHashMap<>();
        consume('{');
        skipWhitespace();

        // Handle empty object
        if (peek() == '}') {
            consume('}');
            return object;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();
            
            skipWhitespace();
            consume(':');
            
            skipWhitespace();
            Object value = parse();
            
            object.put(key, value);
            
            skipWhitespace();
            
            if (peek() == '}') {
                consume('}');
                break;
            }
            
            consume(',');
        }
        
        return object;
    }

    private OroArray parseArray() {
        OroArray array = new OroArray();
        consume('[');
        skipWhitespace();

        // Handle empty array
        if (peek() == ']') {
            consume(']');
            return array;
        }

        while (true) {
            skipWhitespace();
            Object value = parse();
            array.append(value);
            
            skipWhitespace();
            
            if (peek() == ']') {
                consume(']');
                break;
            }
            
            consume(',');
        }
        
        return array;
    }

    private String parseString() {
        consume('"');
        StringBuilder sb = new StringBuilder();
        
        while (peek() != '"') {
            if (peek() == '\\') {
                // Handle escape characters
                advance();
                char escaped = peek();
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(escaped);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    default:
                        throw new RuntimeError("Invalid escape character: '\\" + escaped + "'");
                }
            } else {
                sb.append(peek());
            }
            advance();
        }
        
        consume('"');
        return sb.toString();
    }

    private Boolean parseTrue() {
        consume('t');
        consume('r');
        consume('u');
        consume('e');
        return true;
    }

    private Boolean parseFalse() {
        consume('f');
        consume('a');
        consume('l');
        consume('s');
        consume('e');
        return false;
    }

    private Object parseNull() {
        consume('n');
        consume('u');
        consume('l');
        consume('l');
        return null;
    }

    private Number parseNumber() {
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;

        // Handle optional negative sign
        if (peek() == '-') {
            sb.append(advance());
        }

        // Integer part
        while (isDigit(peek())) {
            sb.append(advance());
        }

        // Fractional part
        if (peek() == '.') {
            isFloat = true;
            sb.append(advance());
            
            while (isDigit(peek())) {
                sb.append(advance());
            }
        }

        // Scientific notation
        if (peek() == 'e' || peek() == 'E') {
            isFloat = true;
            sb.append(advance());
            
            if (peek() == '+' || peek() == '-') {
                sb.append(advance());
            }
            
            while (isDigit(peek())) {
                sb.append(advance());
            }
        }

        String numberStr = sb.toString();
        return isFloat ? Double.parseDouble(numberStr) : Long.parseLong(numberStr);
    }

    // Utility methods
    private void skipWhitespace() {
        while (isWhitespace(peek())) {
            advance();
        }
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return json.charAt(current);
    }

    private char advance() {
        current++;
        return json.charAt(current - 1);
    }

    private void consume(char expected) {
        if (isAtEnd() || peek() != expected) {
            throw new RuntimeError("Unexpected character. Expected '" + expected + "'");
        }
        advance();
    }

    private boolean isAtEnd() {
        return current >= json.length();
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\r' || c == '\n' || c == '\t';
    }

    public static String toJson(Object obj) {
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (!first) sb.append(", ");
                sb.append("\"").append(entry.getKey()).append("\": ").append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else if (obj instanceof List) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Object item : (List<?>) obj) {
                if (!first) sb.append(", ");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        } else if (obj instanceof String) {
            return "\"" + obj + "\"";  // Properly quote strings
        } else if (obj == null) {
            return "null";
        }
        return obj.toString();  // Numbers, booleans
    }
    

    // Public method to parse JSON string
    public static Object parseJSON(String jsonString) {
        return new JSONParser(jsonString).parse();
    }
}