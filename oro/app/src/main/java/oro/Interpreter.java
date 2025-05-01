package oro;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static oro.OroPDFDocument.redactHIPAA;
import static oro.OroPDFDocument.redactHIPAARef;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    // Resolution info from Resolver
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
      initBuiltIns();
    }


    private static void checkArgCount(List<Object> args, int expected, String usage) {
      if (args.size() != expected) {
          throw new RuntimeError("Expected " + expected + " arguments for " + usage);
      }
    }

    private void initBuiltIns(){
      // Native functions with implementation using native Java //
      globals.define("clock", new OroCallable() {
        @Override
        public int arity() { return 0; }
  
        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
          return (double)System.currentTimeMillis() / 1000.0;
        }
  
        @Override
        public String toString() { return "<native function clock>"; }
      });

      // String toUpper(String text)
      globals.define("toUpper", new OroCallable() {
        @Override
        public int arity() { return 1; }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
            try{
              return ((String) arguments.get(0)).toUpperCase();
            }
            catch (ClassCastException e){
              return "OroError: " + arguments.get(0).getClass().getSimpleName() + " cannot be cast as String";
            }
        }

        @Override
        public String toString() { return "<native function toUpper>"; }
    });

      // Boolean matchRegex(String text1, String text2)
      globals.define("matchRegex", new OroCallable() {
        @Override
        public int arity() { return 2; }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
          try {
            String input = (String) arguments.get(0);
            String pattern = (String) arguments.get(1);
            return input.matches(pattern);
            }
            catch (ClassCastException e){
              if (!(arguments.get(0) instanceof String)) {
                return "OroError: " + arguments.get(0).getClass().getSimpleName() + " cannot be cast to String (first argument)";
              } 
              else if (!(arguments.get(1) instanceof String)) {
                return "OroError: " + arguments.get(1).getClass().getSimpleName() + " cannot be cast to String (second argument)";
              }
              return "OroError: Unexpected error in arguments.";            
            }
        }

        @Override
        public String toString() { return "<native function matchRegex>"; }
    });

      // String toLower(String text)
      globals.define("toLower", new OroCallable() {
        @Override
        public int arity() { return 1; }
    
        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
          try{
            return ((String) arguments.get(0)).toLowerCase();
          }
          catch (ClassCastException e){
            return "OroError: " + arguments.get(0).getClass().getSimpleName() + " cannot be cast as String";
          }        }
    
        @Override
        public String toString() { return "<native function toLowerCase>"; }
    });

      globals.define("toString", new OroCallable() {
        @Override
        public int arity() { return 1; }
    
        @Override
        public Object call(Interpreter interpreter, List<Object> args) {
            checkArgCount(args, 1, "toString(value)");
    
            Object value = args.get(0);
            if (value instanceof Double) {
                return value.toString(); // Convert number to string
            } else if (value instanceof Boolean) {
                return value.toString(); // Convert boolean to string
            } else if (value instanceof String) {
                return value; // If it's already a string, return as is
            } else {
                return value.toString(); // For other objects, call their toString method
            }
        }
    
        @Override
        public String toString() { return "<native function toString>"; }
    });

      // String trim(String text)
      globals.define("trim", new OroCallable() {
        @Override
        public int arity() { return 1; }
    
        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
          try{  
            return ((String) arguments.get(0)).trim();
          } 
          catch (ClassCastException e){
            return "OroError: " + arguments.get(0).getClass().getSimpleName() + " cannot be cast as String";
          }
        }
    
        @Override
        public String toString() { return "<native function trim>"; }
    }); 

      // String substring(String text, Int start, Int end)
      globals.define("substring", new OroCallable() {
        @Override
        public int arity() { return 3; }
    
        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
            String text = (String) arguments.get(0);
            int start = ((Double) arguments.get(1)).intValue();
            int end = ((Double) arguments.get(2)).intValue();
            try{
              return text.substring(start, end);
            }
            catch (StringIndexOutOfBoundsException e){
              return "OroError: Error calling substring(String text, Int start, Int end)";
            }
        }

        @Override
        public String toString() { return "<native function substring>"; }
    });
  
      // String replace(String text, String target, String replacement)
      globals.define("replace", new OroCallable() {
        @Override
        public int arity() { return 3; }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
          try{
            String text = (String) arguments.get(0);
            String target = (String) arguments.get(1);
            String replacement = (String) arguments.get(2);
            return text.replace(target, replacement);
          }
          catch (ClassCastException e){
            if (!(arguments.get(0) instanceof String)) {
              return "OroError: " + arguments.get(0).getClass().getSimpleName() + " cannot be cast to String (first argument)";
            } 
            else if (!(arguments.get(1) instanceof String)) {
              return "OroError: " + arguments.get(1).getClass().getSimpleName() + " cannot be cast to String (second argument)";
            }
            else if (!(arguments.get(1) instanceof String)) {
              return "OroError: " + arguments.get(2).getClass().getSimpleName() + " cannot be cast to String (third argument)";
            }
            return "OroError: Unexpected error in arguments.";       
          }
        }

        @Override
        public String toString() { return "<native function replace>"; }
    });
  
      // Boolean contains(String text, String substring)
      globals.define("contains", new OroCallable() {
        @Override
        public int arity() { return 2; }
    
        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
        try{   
          return ((String) arguments.get(0)).contains((String) arguments.get(1));
        }
        catch (ClassCastException e){
          if (!(arguments.get(0) instanceof String)) {
            return "OroError: " + arguments.get(0).getClass().getSimpleName() + " cannot be cast to String (first argument)";
          } 
          else if (!(arguments.get(1) instanceof String)) {
            return "OroError: " + arguments.get(1).getClass().getSimpleName() + " cannot be cast to String (second argument)";
          }
          else {
            return "OroError: Unexpected error in arguments.";
          }
        }
      }
    
        @Override
        public String toString() { return "<native function contains>"; }
    });
  
      // Int length(String text)
      globals.define("length", new OroCallable() {
        @Override
        public int arity() { return 1; }
    
        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
          try{
            return ((String) arguments.get(0)).length();
          }
          catch (ClassCastException e){
            return "OroError: " + arguments.get(0).getClass().getSimpleName() + " cannot be cast to String";
          }
        }
    
        @Override
        public String toString() { return "<native fun length>"; }
    });

    // Double sqrt(Int number)
    globals.define("sqrt", new OroCallable() {
      @Override
      public int arity() { return 1; }
  
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
          try {
              double number = ((Double) arguments.get(0)).doubleValue();
              if (number < 0) {
                  return "OroError: Cannot calculate square root of a negative number.";
              }
              return Math.sqrt(number);
          } catch (ClassCastException e) {
              return "OroError: Argument must be a Number.";
          }
      }
  
      @Override
      public String toString() { return "<native function sqrt>"; }
  });

  // Double abs(Int num)
  globals.define("abs", new OroCallable() {
    @Override
    public int arity() { return 1; }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        try {
            double number = ((Double) arguments.get(0)).doubleValue();
            return Math.abs(number);
        } catch (ClassCastException e) {
            return "OroError: Argument must be a Number.";
        }
    }

    @Override
    public String toString() { return "<native function abs>"; }
});

  
  globals.define("append", new OroCallable() {
    @Override
    public int arity() { return 2; }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (!(arguments.get(0) instanceof OroArray)) {
            System.out.println("OroError: First argument must be an array.");
            return null;
        }
        ((OroArray) arguments.get(0)).append(arguments.get(1));
        return null;
    }

    @Override
    public String toString() { return "<native function append>"; }
  });

  globals.define("size", new OroCallable() {
    @Override
    public int arity() { return 1; }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (!(arguments.get(0) instanceof OroArray)) {
          System.out.println("OroError: Argument must be an array.");
            return null;
        }
        return ((OroArray) arguments.get(0)).size();
    }

    @Override
    public String toString() { return "<native function size>"; }
  });

  globals.define("parseJSON", new OroCallable() {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (arguments.size() != 1 || !(arguments.get(0) instanceof String)) {
            throw new RuntimeError("OroError: parseJSON requires a single string argument");
        }
        return JSONParser.parseJSON((String)arguments.get(0));
    }

    @Override
    public int arity() {
        return 1;
    }
  });

  globals.define("printJSON", new OroCallable() {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        for (Object arg : arguments) {
            System.out.println(JSONParser.toJson(arg));  // Use toJson for proper formatting
        }
        return null;
    }

    @Override
    public int arity() {
        return 1;
    }
  });

  globals.define("type", new OroCallable() {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
      return arguments.get(0).getClass().getSimpleName();
    }

    @Override
    public int arity() {
      return 1;
    }
  });

  globals.define("TxtDocument", new OroCallable() {
    @Override
    public int arity() {
        return 1; // File Path
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (((String) arguments.get(0)).toLowerCase().endsWith(".txt") != true) {
          throw new RuntimeError("Error: Unsupported file type");
        }
        else{
          try{
            return new OroDocument((String) arguments.get(0)); // Returns a new instance of Document
          } 
          catch (IOException e){
            throw new RuntimeError("Error: " + e.getMessage());
          }
        }
        
    }

    @Override
    public String toString() {
        return "<native class TxtDocument>";
    }
  });

  globals.define("getTxtText", new OroCallable() {
    @Override
    public int arity() {
        return 1; // Document Instance 
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (!(arguments.get(0) instanceof OroDocument))  {
          throw new RuntimeError("Error: Expected argument of type TxtDocument");
        }
        else{
          OroDocument doc = (OroDocument) arguments.get(0);
          return doc.getDocText(doc.getFilePath()); // Returns a new instance of Document
        }
        
    }

    @Override
    public String toString() {
        return "<native fun getTxtText>";
    }
  });

  globals.define("createTxtDoc", new OroCallable() {
    @Override
    public int arity() {
        return 2; // Document Instance, replacement text
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (!(arguments.get(0) instanceof String) || !(arguments.get(1) instanceof String))  {
          throw new RuntimeError("Error: Expected argument of type String");
        }
        else{
          //OroDocument doc = (OroDocument) arguments.get(0);
          String filePath = (String) arguments.get(0);
          String replacementText = (String) arguments.get(1);
          // 1 if successful, 0 if not
          OroDocument.createDoc(filePath, replacementText);
          return null; 
        }
    }

    @Override
    public String toString() {
        return "<native fun createTxtDoc>";
    }
  });


        globals.define("PDFDocument", new OroCallable() {
            @Override
            public int arity() {
                return 1; // File Path
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (((String) arguments.get(0)).toLowerCase().endsWith(".pdf") != true) {
                    throw new RuntimeError("Error: Unsupported file type");
                }
                else{
                    try{
                        return new OroPDFDocument((String) arguments.get(0)); // Returns a new instance of Document
                    }
                    catch (IOException e){
                        throw new RuntimeError("Error loading PDF: " + e.getMessage());
                    }
                }

            }

            @Override
            public String toString() {
                return "<native class PDFDocument>";
            }
        });

        globals.define("getPDFText", new OroCallable() {
            @Override
            public int arity() {
                return 1; // Document Instance
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof OroPDFDocument))  {
                    throw new RuntimeError("Error: Expected argument of type PDF");
                }
                else{
                    try {
                        OroPDFDocument pdf = (OroPDFDocument) arguments.get(0);
                        return pdf.getPDFText();
                    } catch (IOException e) {
                        throw new RuntimeException("Error getting PDF text: " + e.getMessage());
                    }
                }

            }

            @Override
            public String toString() {
                return "<native fun getPDFText>";
            }
        });

        globals.define("workingDir", new OroCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return System.getProperty("user.dir");

            }

            @Override
            public String toString() {
                return "<native fun workingDir>";
            }
        });

        globals.define("createPDF", new OroCallable() {
            @Override
            public int arity() {
                return 2; // File path, replacement text
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof String) || !(arguments.get(1) instanceof String))  {
                    throw new RuntimeError("Error: Expected arguments of type String");
                }
                else{

                    String filePath = (String) arguments.get(0);
                    String replacementText = (String) arguments.get(1);
                    // 1 if successful, 0 if not
                    try {
                        OroPDFDocument.createPDF(filePath, replacementText);
                    }
                    catch (IOException e){
                        throw new RuntimeError("Error creating PDF: " + e.getMessage());
                    }
                    return null;
                }
            }

            @Override
            public String toString() {
                return "<native fun createPDF>";
            }
        });

        // Given a PDF instance, redact all information required under HIPAA and save to a new PDF
        globals.define("redactHIPAA", new OroCallable() {
            @Override
            public int arity() {
                return 2; // PDF instance, redacted file path
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof OroPDFDocument) || !(arguments.get(1) instanceof String))  {
                    throw new RuntimeError("Error: Expected arguments of type PDF, String");
                }
                else{

                    OroPDFDocument pdf = (OroPDFDocument) arguments.get(0);
                    String redactedPath = (String) arguments.get(1);
                    // 1 if successful, 0 if not
                    try {
                        redactHIPAA(pdf, redactedPath);
                    }
                    catch (IOException e){
                        throw new RuntimeError("Error redacting HIPAA: " + e.getMessage());
                    }
                    return null;
                }
            }

            @Override
            public String toString() {
                return "<native fun redactHIPAA>";
            }
        });

        // Has optional parameter to act as blank document template
        globals.define("redactHIPAA", new OroCallable() {
            @Override
            public int arity() {
                return 3; // PDF instance form, redacted file path, PDF instance template
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (!(arguments.get(0) instanceof OroPDFDocument) || !(arguments.get(1) instanceof String))  {
                    throw new RuntimeError("Error: Expected arguments of type PDF, String");
                }
                else{

                    OroPDFDocument pdf = (OroPDFDocument) arguments.get(0);
                    String redactedPath = (String) arguments.get(1);
                    OroPDFDocument template = (OroPDFDocument) arguments.get(2);
                    // 1 if successful, 0 if not
                    try {
                        redactHIPAARef(pdf, redactedPath, template);
                    }
                    catch (IOException e){
                        throw new RuntimeError("Error redacting HIPAA: " + e.getMessage());
                    }
                    return null;
                }
            }

            @Override
            public String toString() {
                return "<native fun redactHIPAA>";
            }
        });


    }

    void interpret(List<Stmt> statements) {
      try {
        for (Stmt statement : statements) {
          execute(statement);
        }
      } catch (RuntimeError error) {
        Oro.runtimeError(error);
      }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
      return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
      Object left = evaluate(expr.left);
  
      if (expr.operator.type == TokenType.OR) {
        if (isTruthy(left)) return left;
      } else {
        if (!isTruthy(left)) return left;
      }
  
      return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
      Object object = evaluate(expr.object);
  
      if (!(object instanceof OroInstance)) { 
        throw new RuntimeError(expr.name,
                               "Only instances have fields.");
      }
  
      Object value = evaluate(expr.value);
      ((OroInstance)object).set(expr.name, value);
      return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
      int distance = locals.get(expr);
      OroClass superclass = (OroClass)environment.getAt(distance, "super");

      OroInstance object = (OroInstance)environment.getAt(distance - 1, "this");

      OroFunction method = superclass.findMethod(expr.method.lexeme);

      if (method == null) {
        throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
      }
      return method.bind(object);
    }

    @Override
    public Object visitSelfExpr(Expr.Self expr) {
      return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
      Object right = evaluate(expr.right);
  
      switch (expr.operator.type) {
        case BANG:
        return !isTruthy(right);
        case MINUS:
          checkNumberOperand(expr.operator, right);
          return -(double)right;
      }

      // Unreachable
      return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
      return lookUpVariable(expr.name, expr);
    }

    @Override
    public Object visitFStringExpr(Expr.FString expr) {
      StringBuilder result = new StringBuilder();

      for (Expr part : expr.parts) {
        Object value = evaluate(part);
        result.append(value == null ? "null" : value.toString());
      }

      return result.toString();
    }


    private Object lookUpVariable(Token name, Expr expr) {
      Integer distance = locals.get(expr);
      if (distance != null) {
        return environment.getAt(distance, name.lexeme);
      } else {
        return globals.get(name);
      }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
      }
      
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    // Determine what values are defined to be true or false
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        if (object instanceof Double) return ((Double)object) != 0.0;
        return true;
      }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "null";
    
        if (object instanceof Double) {
          String text = object.toString();
          if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
          }
          return text;
        }
    
        return object.toString();
      }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
      return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
      return expr.accept(this);
    }

    private void execute(Stmt stmt) {
      stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
      locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
      executeBlock(stmt.statements, new Environment(environment));
      return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
      Object superclass = null;
      if (stmt.superclass != null) {
        superclass = evaluate(stmt.superclass);
        if (!(superclass instanceof OroClass)) {
          throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
        }
      }
  

      environment.define(stmt.name.lexeme, null);

      // Create super environment on subclass declaration
      if (stmt.superclass != null) {
        environment = new Environment(environment);
        environment.define("super", superclass);
      }

      // Method declaration --> OroFunction object
      Map<String, OroFunction> methods = new HashMap<>();
      for (Stmt.Function method : stmt.methods) {
        OroFunction function = new OroFunction(method, environment, method.name.lexeme.equals(stmt.name.lexeme));
        methods.put(method.name.lexeme, function);
      }
  
      OroClass klass = new OroClass(stmt.name.lexeme, (OroClass)superclass, methods);

      // Pop super environment after interpreting class methods
      if (superclass != null) {
        environment = environment.enclosing;
      }

      environment.assign(stmt.name, klass);
      return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
      evaluate(stmt.expression);
      return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
      OroFunction function = new OroFunction(stmt, environment, false);
      environment.define(stmt.name.lexeme, function);
      return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
      if (isTruthy(evaluate(stmt.condition))) {
        execute(stmt.thenBranch);
      } else if (stmt.elseBranch != null) {
        execute(stmt.elseBranch);
      }
      return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
      Object value = evaluate(stmt.expression);
      System.out.println(stringify(value));
      return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
      Object value = null;
      if (stmt.value != null) value = evaluate(stmt.value);
      throw new Return(value);
    }
  

    @Override
    public Void visitDefStmt(Stmt.Def stmt) {
      Object value = null;
      if (stmt.initializer != null) {
        value = evaluate(stmt.initializer);
      }
  
      environment.define(stmt.name.lexeme, value);
      return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
      while (isTruthy(evaluate(stmt.condition))) {
        execute(stmt.body);
      }
      return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
      Object value = evaluate(expr.value);
      
      Integer distance = locals.get(expr);
      if (distance != null) {
        environment.assignAt(distance, expr.name, value);
      } else {
        globals.assign(expr.name, value);
      }
  
      return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); 

    switch (expr.operator.type) {
        case GREATER:
          checkNumberOperands(expr.operator, left, right);
          return (double)left > (double)right;
        case GREATER_EQUAL:
          checkNumberOperands(expr.operator, left, right);
          return (double)left >= (double)right;
        case LESS:
          checkNumberOperands(expr.operator, left, right);
          return (double)left < (double)right;
        case LESS_EQUAL:
          checkNumberOperands(expr.operator, left, right);
          return (double)left <= (double)right;
        case BANG_EQUAL: 
          return !isEqual(left, right);
        case EQUAL_EQUAL: 
          return isEqual(left, right);
        case MINUS:
          checkNumberOperands(expr.operator, left, right);
          return (double)left - (double)right;
        case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        } 

        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }

        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
        case SLASH:
          checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
        case STAR:
          checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
    }

    // Unreachable
    return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
      Object callee = evaluate(expr.callee);
  
      List<Object> arguments = new ArrayList<>();
      for (Expr argument : expr.arguments) { 
        arguments.add(evaluate(argument));
      }

      if (!(callee instanceof OroCallable)) {
        throw new RuntimeError(expr.paren,
            "Can only call functions and classes.");
      }

      OroCallable function = (OroCallable)callee;
      if (arguments.size() != function.arity()) {
        throw new RuntimeError(expr.paren, "Expected " +
            function.arity() + " arguments but got " +
            arguments.size() + ".");
      }
      return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
      Object object = evaluate(expr.object);
      if (object instanceof OroInstance) {
        return ((OroInstance) object).get(expr.name);
      }
  
      throw new RuntimeError(expr.name,
          "Only instances have properties.");
    }

    @Override
    public Object visitArrayLiteralExpr(Expr.ArrayLiteral expr) {
        List<Object> values = new ArrayList<>();
        for (Expr element : expr.elements) {
            values.add(evaluate(element));
        }
        return new OroArray(values);
    }

    @Override
    public Object visitIndexExpr(Expr.Index expr) {
        Object array = evaluate(expr.array);
        Object index = evaluate(expr.index);

        if (array instanceof Map) {
        // Safe suppress since the only way array isinstance of map is if parsed as a Json (String, Object)by JSONParser
        @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) array;
          if (!(index instanceof String)) {
              return "Object keys must be strings.";
          }
          return map.get(index);
      }
        
        
        if (!(array instanceof OroArray)) {
            return "Can only index into arrays.";
        }
        if (!(index instanceof Double)) {
            return "Array index must be a number.";
        }

        return ((OroArray) array).get((int) ((double) index));
    }

  @Override
  public Object visitIndexAssignExpr(Expr.IndexAssign expr) {
      Object array = evaluate(expr.array);
      Object index = evaluate(expr.index);
      Object value = evaluate(expr.value);

      if (array instanceof Map) {
        // Safe suppress since the only way array isinstance of map is if parsed as a Json (String, Object)by JSONParser
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) array;
        if (!(index instanceof String)) {
            return "Object keys must be strings.";
        }
        map.put((String) index, value);
        return value;
    }

      if (!(array instanceof OroArray)) {
          throw new RuntimeError(expr.equals, "Can only index into arrays.");
      }
      if (!(index instanceof Double)) {
          throw new RuntimeError(expr.equals, "Array index must be a number.");
      }

      OroArray oroarray = (OroArray) array;
      List<Object> arrayList = oroarray.getArray();
      int idx = ((Double) index).intValue();

      if (idx < 0 || idx >= arrayList.size()) {
          throw new RuntimeError(expr.equals, "Index out of bounds.");
      }

      arrayList.set(idx, value);
      return value;
  }

    


}
