package com.github.mike10004.nativehelper.subprocess;

/**
 * Exception class that represents an error during subprocess launch.
 */
public class ProcessLaunchException extends ProcessException {

    public ProcessLaunchException(String message) {
        super(message);
    }

    public ProcessLaunchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessLaunchException(Throwable cause) {
        super(cause);
    }
}
