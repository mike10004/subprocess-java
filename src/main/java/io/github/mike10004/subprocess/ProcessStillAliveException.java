package io.github.mike10004.subprocess;

/**
 * Exception thrown when an attempt to kill a process does not succeed before a timeout elapses.
 */
public class ProcessStillAliveException extends SubprocessException {

    public ProcessStillAliveException(String message) {
        super(message);
    }

}
