package io.github.mike10004.subprocess;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BasicProcessTracker implements ProcessTracker {

    private final Set<Process> processes;

    /**
     * Constructs an instance with the default timeout.
     */
    public BasicProcessTracker() {
        // we can probably remove this synchro wrapper because all our methods are synchronized
        this.processes = Collections.synchronizedSet(new HashSet<>());
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

    /**
     * Attempts to destroy all processes tracked by this instance that are still executing.
     * @param destroyTimeoutMillis millseconds to wait for processes to terminate
     * @return the list of processes still alive after the timeout elapses
     * @see ProcessTracker#destroyAll(Iterable, long, TimeUnit)
     */
    public List<Process> destroyAll(long destroyTimeoutMillis) {
        return ProcessTracker.destroyAll(processes, destroyTimeoutMillis, TimeUnit.MILLISECONDS);
    }

}
