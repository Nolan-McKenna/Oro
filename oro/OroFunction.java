package oro;

import java.util.List;

class OroFunction implements OroCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean isInitializer;

  OroFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.declaration = declaration;
  }

  OroFunction bind(OroInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("self", instance);
    return new OroFunction(declaration, environment, isInitializer);
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  // Every function call gets own environment to store locally declared variables
  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme,
          arguments.get(i));
    }

    // Discard function local environment
    try {
        interpreter.executeBlock(declaration.body, environment);
      } catch (Return returnValue) {
        if (isInitializer) return closure.getAt(0, "self");
        return returnValue.value;
      }

    if (isInitializer) return closure.getAt(0, "self");
    return null;
  }

}