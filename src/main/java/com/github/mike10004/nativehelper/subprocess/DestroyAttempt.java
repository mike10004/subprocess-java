package com.github.mike10004.nativehelper.subprocess;

import java.util.concurrent.TimeUnit;

/**
 * Interface that represents an attempt to destroy a process. Destroying
 * a process is inherently asynchronous. The {@link ProcessDestructor}
 * {@code sendXXXSignal()} methods return instances of this class, whose
 * extensions {@link TermAttempt} and {@link KillAttempt} allow a client
 * to wait until a process is actually destroyed.
 */
@SuppressWarnings("unused")
public interface DestroyAttempt {

    /**
     * Gets the result of the attempt.
     * @return the result
     */
    DestroyResult result();

    /**
     * Interface that represents an attempt to destroy a process non-forcibly.
     * @see Process#destroy()
     */
    interface TermAttempt extends DestroyAttempt {

        /**
         * Awaits termination of the process, blocking on the current thread.
         * Unless the waiting is interrupted, the process will be terminated
         * when this method returns.
         * @return a termination attempt instance
         */
        TermAttempt await();

        /**
         * Awaits termination of the process up until a timeout, blocking on the
         * current thread. This method returns when the process terminates, the
         * timeout elapses, or the current thread is interrupted. In the latter
         * two cases, the process may not have terminated.
         * @param duration the timeout duration
         * @param unit the timeout duration unit
         * @return a termination attempt instance
         */
        TermAttempt timeout(long duration, TimeUnit unit);

        /**
         * Attempts to destroy the process forcibly.
         * @return a kill attempt instance
         * @see Process#destroyForcibly()
         */
        KillAttempt kill();
    }

    /**
     * Interface that represents an attempt to destroy a process forcibly.
     * @see Process#destroyForcibly()
     */
    interface KillAttempt extends DestroyAttempt {
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
         * @return true if the process terminated before the timeout elapsed
         */
        boolean timeoutKill(long duration, TimeUnit timeUnit);

        /**
         * Awaits termination of the process, blocking on this thread and throwing an
         * exception if the timeout is exceeded.
         * @param duration the duration to wait
         * @param timeUnit the time unit of the duration value
         * @throws ProcessStillAliveException if the process is not terminated after the
         * timeout elapses
         */
        void timeoutOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException;
    }

}
