package io.github.mike10004.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface that represents a process context. A process context tracks the creation
 * and destruction of processes.
 */
public interface ProcessTracker {

    /**
     * Adds a process to this context instance.
     * @param process the process
     */
    void add(Process process);

    /**
     * Removes a process from this context instance.
     * @param process the process to remove
     * @return true iff the process existed in this process and was removed
     * @throws RuntimeException if process exists in this context and removing it failed
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean remove(Process process);

    /**
     * Gets the count of process that have been added to this context but not removed.
     * @return the count of active processes
     */
    int activeCount();

    /**
     * Attempts to destroy multiple processes. For each process, {@link Process#destroy()} is invoked,
     * and then {@link Process#waitFor(long, TimeUnit) waitFor()} is invoked, blocking on the current
     * thread until the process terminates or the timeout elapses. If the process is still alive
     * after that wait, {@link Process#destroyForcibly() destroyForcibly()} is invoked, and the wait
     * is repeated.
     * @param processes the processes to destroy
     * @param timeoutPerProcess the timeout per process (total wait time is potentially twice this duration)
     * @param unit the timeout unit
     * @return a list of processes that are still alive
     */
    static List<Process> destroyAll(Iterable<Process> processes, long timeoutPerProcess, TimeUnit unit) {
        Logger log = LoggerFactory.getLogger(ProcessTracker.class);
        List<Process> undestroyed = new ArrayList<>();
        for (Process p : Defensive.listOf(processes)) {
            p.destroy();
            boolean terminated = false;
            try {
                terminated = p.waitFor(timeoutPerProcess, unit);
            } catch (InterruptedException e) {
                log.warn("interrupted while waiting for process to terminate by destroy()");
            }
            if (!terminated) {
                p.destroyForcibly();
                try {
                    p.waitFor(timeoutPerProcess, unit);
                } catch (InterruptedException e) {
                    log.warn("interrupted while waiting for process to terminate by destroyForcibly()");
                }
            }
            if (p.isAlive()) {
                LoggerFactory.getLogger(ProcessTracker.class).error("failed to terminated process " + p);
                undestroyed.add(p);
            }
        }
        return undestroyed;

    }
}
