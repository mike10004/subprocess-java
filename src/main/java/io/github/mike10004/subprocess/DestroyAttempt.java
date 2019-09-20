package io.github.mike10004.subprocess;

/**
 * Interface that represents an attempt to destroy a process. Destroying
 * a process is an asynchronous action, in that you send a signal to a
 * process but that action does not return any feedback.
 * Extensions of this interface allow the caller
 * to wait until a process is actually destroyed.
 * @see SigkillAttempt
 * @see SigtermAttempt
 */
public interface DestroyAttempt {

    /**
     * Gets the result of the attempt.
     * @return the result
     */
    DestroyResult result();

}
