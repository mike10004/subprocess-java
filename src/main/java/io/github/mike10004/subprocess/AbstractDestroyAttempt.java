package io.github.mike10004.subprocess;

import static java.util.Objects.requireNonNull;

/**
 * Superclass for destroy attempts.
 */
public class AbstractDestroyAttempt implements DestroyAttempt {

    protected final DestroyResult result;
    protected final Process process;

    public AbstractDestroyAttempt(DestroyResult result, Process process) {
        this.result = requireNonNull(result);
        this.process = requireNonNull(process);
    }

    /**
     * Gets the result of the attempt.
     * @return the result
     */
    @Override
    public DestroyResult result() {
        return result;
    }

}
