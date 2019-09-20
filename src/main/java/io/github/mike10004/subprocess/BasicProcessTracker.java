package io.github.mike10004.subprocess;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service class that implements basic process tracking. An instance of this
 * class maintains a set of processes that have been launched
 */
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
        boolean added = processes.add(process);
        if (!added && !processes.contains(process)) {
            throw new IllegalStateException("failed to add " + process);
        }
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
     * @param destroyTimeoutMillis millseconds to wait for each process to terminate
     * @return the list of processes still alive after the timeout elapses
     * @see ProcessTracker#destroyAll(Iterable, long, TimeUnit)
     */
    public List<Process> destroyAll(long timeoutPerProcess, TimeUnit unit) {
        return ProcessTracker.destroyAll(processes, timeoutPerProcess, unit);
    }

}
