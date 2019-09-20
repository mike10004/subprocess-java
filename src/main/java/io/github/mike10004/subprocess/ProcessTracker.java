package io.github.mike10004.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface of a service that tracks processes. Implementations of this interface
 * track the creation and destruction of processes.
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
     * Attempts to destroy multiple processes. For each process, the following procedure is performed:
     * <ul>
     *     <li>check whether process has already terminated</li>
     *     <li>if process is still alive, {@link Process#destroy()} is invoked, and then the specified timeout is waited</li>
     *     <li>if process is still alive, {@link Process#destroyForcibly()} is invoked</li>
 *     </ul>
     * and then {@link Process#waitFor(long, TimeUnit) waitFor()} is invoked, blocking on the current
     * thread until the process terminates or the timeout elapses. If the process is still alive
     * after that wait, {@link Process#destroyForcibly() destroyForcibly()} is invoked, and the wait
     * is repeated.
     * @param processes the processes to destroy
     * @param timeoutPerProcess the timeout per process (total wait time per process may be longer)
     * @param unit the timeout unit
     * @return a list of processes that are still alive
     */
    static List<Process> destroyAll(Iterable<Process> processes, long timeoutPerProcess, TimeUnit unit) {
        Logger log = LoggerFactory.getLogger(ProcessTracker.class);
        List<Process> undestroyed = new ArrayList<>();
        for (Process p : Defensive.immutableCopyOf(processes)) {
            p.destroy();
            boolean endedNaturally = false, destroyed = false, destroyedForcibly = false;
            // first check that the process isn't already terminated by waiting briefly for it to end
            try {
                endedNaturally = p.waitFor(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("interrupted while waiting for process to terminate by destroy()");
            }
            if (!endedNaturally && p.isAlive()) {
                p.destroy();
                try {
                    // wait the specified timeout for the process to clean up in response to SIGTERM
                    destroyed = p.waitFor(timeoutPerProcess, unit);
                } catch (InterruptedException e) {
                    log.warn("interrupted while waiting for process to terminate by destroy()");
                }
                if (!destroyed && p.isAlive()) {
                    p.destroyForcibly();
                    try {
                        // wait just a little longer
                        p.waitFor(5, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        log.warn("interrupted while waiting for process to terminate by destroyForcibly()");
                    }
                    destroyedForcibly = !p.isAlive();
                }
            }
            if (p.isAlive()) {
                LoggerFactory.getLogger(ProcessTracker.class).error("failed to terminated process " + p);
                undestroyed.add(p);
            } else {
                int code = p.exitValue();
                String method = endedNaturally
                        ? "ended naturally"
                        : (destroyed ? "destroyed" : (destroyedForcibly ? "forcibly destroyed" : "indeterminate"));
                log.debug("{} terminated with exit code {}; {}", p, code, method);
            }
        }
        return undestroyed;
    }
}
