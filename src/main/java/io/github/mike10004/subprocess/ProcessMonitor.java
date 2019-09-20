package io.github.mike10004.subprocess;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Interface of a process monitor. A process monitor is returned by
 * {@link SubprocessLauncher#launch(Subprocess)} or {@link SubprocessLaunchSupport#launch()}.
 *
 * Processes are launched asynchronously, and launch methods return
 * a monitor instance.
 *
 * @param <SO> type of captured standard output contents
 * @param <SE> type of captured standard error contents
 */
public interface ProcessMonitor<SO, SE> {

    /**
     * Gets the underlying future corresponding to this monitor.
     * @return the result future
     */
    Future<ProcessResult<SO, SE>> future();

    /**
     * Gets a destructor that acts upon the process being monitored.
     * @return a destructor instance
     */
    ProcessDestructor destructor();

    /**
     * Gets the process being monitored.
     * @return the process
     */
    Process process();

    /**
     * Gets the process tracker instance that was used to launch the process.
     * @return the process tracker
     */
    @SuppressWarnings("unused")
    ProcessTracker tracker();

    /**
     * Blocks on this thread, waiting for the process to finish or the given timeout to elapse.
     * @param timeout the timeout
     * @param unit the timeout duration unit
     * @return the process result
     * @throws TimeoutException if the timeout elapses before the process finishes
     * @throws InterruptedException if the waiting is interrupted
     * @throws SubprocessExecutionException if an execution exception is thrown on the process execution thread
     */
    ProcessResult<SO, SE> await(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SubprocessExecutionException;

    /**
     * Blocks on this thread, waiting for the process to finish.
     * Avoid using this method; favor the overload that accepts a timeout, as
     * it's probably never a good idea to wait forever for anything.
     * @return the process result
     * @throws InterruptedException if the waiting is interrupted
     * @throws SubprocessExecutionException if an execution exception is thrown on the process execution thread
     */
    ProcessResult<SO, SE> await() throws InterruptedException, SubprocessExecutionException;
}
