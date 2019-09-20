package io.github.mike10004.subprocess;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.mike10004.subprocess.Preconditions.checkArgument;

/**
 * Implementation of a process tracker that can be used in a try-with-resources
 * block. When execution exits the block, all the processes are destroyed
 * using {@link #destroyAll()}. This is probably the best implementation of a
 * process tracker to use for cases where you are waiting synchronously for
 * processes to execute.
 *
 * <p>For the asynchronous use case, use {@link BasicProcessTracker}.
 */
public class ScopedProcessTracker extends BasicProcessTracker implements AutoCloseable {

    /**
     * Default amount of time to wait for processes to terminate when {@link #close()} is invoked.
     */
    public static final long DEFAULT_DESTROY_TIMEOUT_MILLIS = 500;

    private final long destroyTimeoutPerProcessMs;

    /**
     * Constructs an instance with the default timeout.
     */
    public ScopedProcessTracker() {
        this(DEFAULT_DESTROY_TIMEOUT_MILLIS);
    }

    /**
     * Constructs an instance that will use the given timeout when attempting to destroy processes.
     * @param destroyTimeoutPerProcessMs time to wait before throwing an exception on close
     */
    public ScopedProcessTracker(long destroyTimeoutPerProcessMs) {
        this.destroyTimeoutPerProcessMs = destroyTimeoutPerProcessMs;
        checkArgument(destroyTimeoutPerProcessMs >= 0, "timeout must be nonnegative");
    }

    /**
     * Attempts to destroy all processes tracked by this instance that are still executing.
     * @return the list unfinished processes
     * @see ProcessTracker#destroyAll(Iterable, long, TimeUnit)
     */
    public List<Process> destroyAll() {
        return super.destroyAll(destroyTimeoutPerProcessMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to destroy all tracked processes that are still executing.
     * @throws ProcessStillAliveException if processes are still alive after attempting
     * to destroy them and waiting for the timeout to elapse
     */
    @Override
    public synchronized void close() throws ProcessStillAliveException {
        List<Process> undestroyed = destroyAll();
        List<Process> stillAlive = undestroyed.stream().filter(Process::isAlive).collect(Collectors.toList());
        int stillAliveCount = stillAlive.size();
        if (stillAliveCount > 0) {
            throw new ProcessStillAliveException(stillAliveCount + " processes still alive after attempting to destroy them");
        }
    }

}
