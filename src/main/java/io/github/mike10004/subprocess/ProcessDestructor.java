package io.github.mike10004.subprocess;

/**
 * Service class used to destroy a single process.
 */
public interface ProcessDestructor {

    /**
     * Sends SIGTERM or equivalent to a process.
     * @return a termination attempt instance
     */
    DestroyAttempt.TermAttempt sendTermSignal();

    /**
     * Sends SIGKILL or equivalent to a process.
     * @return a kill attempt instance
     */
    DestroyAttempt.KillAttempt sendKillSignal();

}
