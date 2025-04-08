package oro;

class RuntimeError extends RuntimeException {
    final Token token;
    private static final long serialVersionUID = 1L;
  
    RuntimeError(Token token, String message) {
      super(message);
      this.token = token;
    }

    // Message Only RuntimeError
    RuntimeError(String message) {
      super(message);
      this.token = null;
    }
  }