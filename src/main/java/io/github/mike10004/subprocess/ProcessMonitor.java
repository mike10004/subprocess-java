package io.github.mike10004.subprocess;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Interface of a process monitor. A process monitor is returned by
 * {@link Subprocess#launch(ProcessTracker, StreamContext)} or
 * {@link Subprocess.Launcher#launch()}. Process execution is inherently asynchronous, so
 * launching a monitor returns an object that allows you to decide how to
 * handle changes in the process's state. To attach a listener
 * to the process, grab the {@link #future() future} and
 * {@link com.google.common.util.concurrent.Futures#addCallback(ListenableFuture, FutureCallback, Executor) Futures.addCallback()}.
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
     * @see Subprocess#launch(ProcessTracker, StreamContext)
     * @see Subprocess#launcher(ProcessTracker)
     */
    @SuppressWarnings("unused")
    ProcessTracker tracker();

    /**
     * Exception that wraps an execution exception when acquiring an asynchronous result.
     * @see java.util.concurrent.Future#get()
     * @see java.util.concurrent.ExecutionException
     */
    class ProcessExecutionInnerException extends Subprocess.SubprocessExecutionException {
        public ProcessExecutionInnerException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Blocks on this thread, waiting for the process to finish or the given timeout to elapse.
     * @param timeout the timeout
     * @param unit the timeout duration unit
     * @return the process result
     * @throws TimeoutException if the timeout elapses before the process finishes
     * @throws InterruptedException if the waiting is interrupted
     * @throws ProcessExecutionInnerException if there is an execution exception in the process execution thread
     */
    ProcessResult<SO, SE> await(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, ProcessExecutionInnerException;

    /**
     * Blocks on this thread, waiting for the process to finish.
     * @return the process result
     * @throws SubprocessException if there is an execution exception in the process execution thread
     * @throws InterruptedException if the waiting is interrupted
     */
    ProcessResult<SO, SE> await() throws SubprocessException, InterruptedException;
}
