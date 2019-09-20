package io.github.mike10004.subprocess;

/**
 * Exception class that represents an error during subprocess launch.
 * One common cause of such an error is that the executable could not be found.
 * Another is that the working directory does not exist at the time of launch
 */
public class SubprocessLaunchException extends SubprocessException {

    public SubprocessLaunchException(String message) {
        super(message);
    }

    public SubprocessLaunchException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubprocessLaunchException(Throwable cause) {
        super(cause);
    }
}
