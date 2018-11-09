package io.github.mike10004.subprocess;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.mike10004.subprocess.Preconditions.checkState;

/**
 * Implementation of a process tracker that can be used in a try-with-resources
 * block. When execution exits the block, all the processes are destroyed
 * using {@link #destroyAll()}.
 */
public class ScopedProcessTracker implements ProcessTracker, AutoCloseable {

    public static final long DEFAULT_DESTROY_TIMEOUT_MILLIS = 500;

    private final Set<Process> processes;
    private final long destroyTimeoutMillis;

    public ScopedProcessTracker() {
        this(DEFAULT_DESTROY_TIMEOUT_MILLIS);
    }

    ScopedProcessTracker(long destroyTimeoutMillis) {
        // we can probably remove this synchro wrapper because all our methods are synchronized
        this.processes = Collections.synchronizedSet(new HashSet<>());
        this.destroyTimeoutMillis = destroyTimeoutMillis;
    }

    @Override
    public synchronized void add(Process process) {
        processes.add(process);
        Preconditions.checkState(processes.contains(process), "failed to add %s", process);
    }

    @Override
    public synchronized boolean remove(Process process) {
        return processes.remove(process);
    }

    @Override
    public synchronized int activeCount() {
        return processes.size();
    }

    public List<Process> destroyAll() {
        return ProcessTracker.destroyAll(processes, destroyTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void close() throws ProcessStillAliveException {
        List<Process> undestroyed = destroyAll();
        List<Process> stillAlive = undestroyed.stream().filter(Process::isAlive).collect(Collectors.toList());
        processes.retainAll(stillAlive);
        int stillAliveCount = stillAlive.size();
        if (stillAliveCount > 0) {
            throw new ProcessStillAliveException(stillAliveCount + " processes still alive");
        }
    }


}
