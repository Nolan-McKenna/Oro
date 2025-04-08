package oro;

import java.util.List;

public class RegexFunctions {

    public static void registerAll(Interpreter interpreter) {
        interpreter.globals.define("regex_match", new OroCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                checkArgCount(args, 2, "regex_match(pattern, input)");
                return new Regex(args.get(0).toString()).matches(args.get(1).toString());
            }

            @Override
            public String toString() { return "<regex function regex_match>"; }
        });

        interpreter.globals.define("regex_find", new OroCallable() {
            @Override
            public int arity() { return 3; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                checkArgCount(args, 3, "regex_find(pattern, input, limit)");

                String patternName = args.get(0).toString();
                String pattern = RegexPatterns.getPattern(patternName); // Get the pattern by name
                if (pattern == null) {
                    pattern = patternName;
                }

                String input = args.get(1).toString();
                int limit = ((Double) args.get(2)).intValue(); // Convert limit to integer
                return new Regex(pattern).find(input, limit);
            }

            @Override
            public String toString() { return "<regex function regex_find>"; }
        });

        interpreter.globals.define("regex_find_all", new OroCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                checkArgCount(args, 2, "regex_find_all(pattern, input)");
                String patternName = args.get(0).toString();
                String pattern = RegexPatterns.getPattern(patternName); 
                if (pattern == null) {
                    pattern = patternName;
                }

                String text = args.get(1).toString();
                return Regex.findAll(pattern, text);
            }

            @Override
            public String toString() { return "<regex function regex_find_all>"; }
        });

        interpreter.globals.define("regex_replace", new OroCallable() {
            @Override
            public int arity() { return 4; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                checkArgCount(args, 4, "regex_replace(pattern, replacement, input, limit)");
                String patternName = args.get(0).toString();
                String pattern = RegexPatterns.getPattern(patternName); // Get the pattern name
                if (pattern == null) {
                    pattern = patternName;
                }

                String replacement = args.get(1).toString();
                String input = args.get(2).toString();
                int limit = ((Double) args.get(3)).intValue();
                return new Regex(pattern).replace(input, replacement, limit);
            }

            @Override
            public String toString() { return "<regex function regex_replace>"; }
        });

        interpreter.globals.define("regex_replace_all", new OroCallable() {
            @Override
            public int arity() { return 3; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                checkArgCount(args, 3, "regex_replace_all(pattern, replacement, input)");
                
                String patternName = args.get(0).toString();
                String pattern = RegexPatterns.getPattern(patternName); // Get the pattern by name
                if (pattern == null) {
                    pattern = patternName;
                }

                String replacement = args.get(1).toString();
                String input = args.get(2).toString();
                return Regex.replaceAll(pattern, replacement, input);
            }

            @Override
            public String toString() { return "<regex function regex_replace_all>"; }
        });
    }

    private static void checkArgCount(List<Object> args, int expected, String usage) {
        if (args.size() != expected) {
            throw new RuntimeError("Expected " + expected + " arguments for " + usage);
        }
    }
}
