// Any callable object must implement this interface

package oro;

import java.util.List;

interface OroCallable {
  int arity();
  Object call(Interpreter interpreter, List<Object> arguments);
}