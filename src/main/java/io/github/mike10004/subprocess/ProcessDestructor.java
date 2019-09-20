package io.github.mike10004.subprocess;

/**
 * Interface that provides methods to destroy a process.
 */
public interface ProcessDestructor {

    /**
     * Sends SIGTERM or equivalent to a process.
     * @return a termination attempt instance
     */
    SigtermAttempt sendTermSignal();

    /**
     * Sends SIGKILL or equivalent to a process.
     * @return a kill attempt instance
     */
    SigkillAttempt sendKillSignal();

}
