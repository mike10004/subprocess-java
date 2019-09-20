package io.github.mike10004.subprocess;

/**
 * Exception thrown when an attempt is made to kill a process within a
 * timeout but the process is still alive after the timeout elapses.
 */
public class ProcessStillAliveException extends SubprocessException {

    public ProcessStillAliveException(String message) {
        super(message);
    }
}
