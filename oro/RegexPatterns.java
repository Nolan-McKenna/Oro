package oro;

import java.util.HashMap;
import java.util.Map;

public class RegexPatterns {
    private static final Map<String, String> namedPatterns = new HashMap<>();

    static {
        namedPatterns.put("USPhoneNumber", "((\\(\\d{3}\\) ?)|(\\d{3}-))?\\d{3}-\\d{4}"); 
        namedPatterns.put("USSSN", "\\d{3}-\\d{2}-\\d{4}"); 
        namedPatterns.put("Email", "[A-Za-z0-9\\._%+\\-]+@[A-Za-z0-9\\.\\-]+\\.[A-Za-z]{2,}"); // Example: test@example.com
        namedPatterns.put("Address", "(\\d{1,}) [a-zA-Z0-9\s]+(\\,)? [a-zA-Z]+(\\,)? [A-Z]{2} [0-9]{5,6}"); 
        namedPatterns.put("CreditCardNumber", "\\b(?:\\d[ -]*?){13,16}\\b"); 
        
    }

    // Get the regex pattern by name
    public static String getPattern(String patternName) {
        return namedPatterns.getOrDefault(patternName, null);
    }
}
