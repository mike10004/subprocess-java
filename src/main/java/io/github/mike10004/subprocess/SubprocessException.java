package io.github.mike10004.subprocess;

/**
 * Superclass for exceptions relating to subprocess operations.
 */
public class SubprocessException extends RuntimeException {

    public SubprocessException(String message) {
        super(message);
    }

    public SubprocessException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubprocessException(Throwable cause) {
        super(cause);
    }
}
