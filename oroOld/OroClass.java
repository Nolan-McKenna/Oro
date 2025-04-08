package oro;

import java.util.List;
import java.util.Map;

class OroClass implements OroCallable {
  final String name;
  final OroClass superclass;
  private final Map<String, OroFunction> methods;

  OroClass(String name, OroClass superclass, Map<String, OroFunction> methods) {
    this.superclass = superclass;
    this.name = name;
    this.methods = methods;
  }

  OroFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }

    if (superclass != null) {
      return superclass.findMethod(name);
    }

    return null;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    OroInstance instance = new OroInstance(this);
    OroFunction initializer = findMethod(this.name);
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments);
    }
    return instance;
  }

  @Override
  public int arity() {
    OroFunction initializer = findMethod(this.name);
    if (initializer == null) return 0;
    return initializer.arity();
  }
}