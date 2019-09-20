package io.github.mike10004.subprocess;

import java.util.concurrent.TimeUnit;

/**
 * Interface that represents an attempt to destroy a process non-forcibly.
 * This interface provides a fluent way to send a SIGTERM, wait for it
 * to succeed, and send a SIGKILL if it does not. A
 * {@link ProcessDestructor process destructor}, acquired from a
 * {@link ProcessMonitor process monitor}, returns instances of this
 * interface.
 * @see Process#destroy()
 */
public interface SigtermAttempt extends DestroyAttempt {

    /**
     * Awaits termination of the process, blocking on the current thread.
     * Unless the waiting is interrupted, the process will be terminated
     * when this method returns.
     * @return a termination attempt instance
     */
    SigtermAttempt await();

    /**
     * Awaits termination of the process up until a timeout, blocking on the
     * current thread. This method returns when the process terminates, the
     * timeout elapses, or the current thread is interrupted. In the latter
     * two cases, the process may not have terminated.
     * @param duration the timeout duration
     * @param unit the timeout duration unit
     * @return a termination attempt instance
     */
    SigtermAttempt await(long duration, TimeUnit unit);

    /**
     * Attempts to destroy the process forcibly.
     * @return a kill attempt instance
     * @see Process#destroyForcibly()
     */
    SigkillAttempt kill();
}
