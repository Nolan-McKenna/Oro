package oro;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Regex {
    private final Pattern pattern;

    public Regex(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public boolean matches(String text) {
        try{
            return pattern.matcher(text).matches();
        }
        catch (PatternSyntaxException e){
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }

    public List<String> find(String text) {
        try{

        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
        }
        catch (PatternSyntaxException e){
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }

    public List<String> find(String text, int limit) {
        try{
            List<String> matches = new ArrayList<>();
            Matcher matcher = pattern.matcher(text);
            int count = 0;
            while (matcher.find() && count < limit) {
                matches.add(matcher.group());
                count++;
            }
            return matches;
        }
        catch (PatternSyntaxException e){
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }

    public String replace(String text, String replacement) {
        try{
            return pattern.matcher(text).replaceAll(replacement);
        }
        catch (PatternSyntaxException e){
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }

    public String replace(String text, String replacement, int limit) {
        try {
            Matcher matcher = pattern.matcher(text);
            StringBuffer sb = new StringBuffer();
            int count = 0;
            while (matcher.find() && count < limit) {
                matcher.appendReplacement(sb, replacement);
                count++;
            }
            matcher.appendTail(sb); // Append the remaining part of the text
            return sb.toString();
        } catch (PatternSyntaxException e) {
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }

    public static boolean match(String regex, String text) {
        try{
            return Pattern.matches(regex, text);
        }
        catch (PatternSyntaxException e){
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }

    public static List<String> findAll(String regex, String text) {
        try{
            return new Regex(regex).find(text);
        }
        catch (PatternSyntaxException e){
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }

    public static String replaceAll(String regex, String replacement, String text) {
        try{
            return new Regex(regex).replace(text, replacement);
        }
        catch (PatternSyntaxException e){
            throw new RuntimeError("Invalid Regex Pattern: " + e.getDescription());
        }
    }
}
