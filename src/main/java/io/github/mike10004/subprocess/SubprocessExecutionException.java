package io.github.mike10004.subprocess;

/**
 * Exception thrown if an error occurs during process execution.
 * This wraps an {@link java.util.concurrent.ExecutionException} thrown when calling
 * {@link java.util.concurrent.Future#get() } to acquire an asynchronous result.
 */
@SuppressWarnings("unused")
public class SubprocessExecutionException extends SubprocessException {

    public SubprocessExecutionException(String message) {
        super(message);
    }

    public SubprocessExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubprocessExecutionException(Throwable cause) {
        super(cause);
    }

}
