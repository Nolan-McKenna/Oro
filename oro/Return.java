// Return exception class used to return down the call stack
package oro;
class Return extends RuntimeException {
    final Object value;
    private static final long serialVersionUID = 1L;
  
    Return(Object value) {
      super(null, null, false, false);
      this.value = value;
    }
  }