package io.github.mike10004.subprocess;

import java.util.concurrent.TimeUnit;

/**
 * Interface that represents an attempt to destroy a process forcibly.
 * @see Process#destroyForcibly()
 */
public interface SigkillAttempt extends DestroyAttempt {
    /**
     * Awaits termination of the process, blocking on this thread.
     * @throws InterruptedException if waiting is interrupted
     */
    void awaitKill() throws InterruptedException;

    /**
     * Awaits termination of the process, blocking on this thread but with a timeout.
     * Interrupted exceptions are ignored and treated as though the timeout elapsed
     * before the kill completed.
     * @param duration the duration to wait
     * @param timeUnit the time unit of the duration value
     * @return an attempt instance representing the state after the process terminates or the timeout elapses
     */
    SigkillAttempt tryAwaitKill(long duration, TimeUnit timeUnit);

    /**
     * Awaits termination of the process, blocking on this thread and throwing an
     * exception if the timeout is exceeded.
     * @param duration the duration to wait
     * @param timeUnit the time unit of the duration value
     * @throws ProcessStillAliveException if the process is not terminated after the
     * timeout elapses
     */
    void awaitOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException;
}
