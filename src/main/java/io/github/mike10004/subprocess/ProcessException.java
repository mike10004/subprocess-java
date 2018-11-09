package io.github.mike10004.subprocess;

/**
 * Superclass for exceptions relating to subprocess operations.
 */
public class ProcessException extends RuntimeException {

    public ProcessException(String message) {
        super(message);
    }

    public ProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessException(Throwable cause) {
        super(cause);
    }
}
