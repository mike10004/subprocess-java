package io.github.mike10004.subprocess;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

/**
 * Service class that implements a process monitor.
 * @param <SO> standard output type
 * @param <SE> standard error type
 */
class BasicProcessMonitor<SO, SE> implements ProcessMonitor<SO, SE> {

    private final Process process;
    private final Future<ProcessResult<SO, SE>> future;
    private final ProcessTracker processTracker;

    BasicProcessMonitor(Process process, Future<ProcessResult<SO, SE>> future, ProcessTracker processTracker) {
        this.future = requireNonNull(future);
        this.process = requireNonNull(process);
        this.processTracker = requireNonNull(processTracker);
    }

    @Override
    public Future<ProcessResult<SO, SE>> future() {
        return future;
    }

    @Override
    public ProcessDestructor destructor() {
        return new BasicProcessDestructor(process, processTracker);
    }

    @Override
    public Process process() {
        return process;
    }

    @Override
    public ProcessTracker tracker() {
        return processTracker;
    }

    @Override
    public ProcessResult<SO, SE> await(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SubprocessExecutionException {
        try {
            boolean ok = process.waitFor(timeout, unit);
            if (!ok) {
                throw new TimeoutException("process.waitFor timeout elapsed");
            }
            return future().get();
        } catch (ExecutionException e) {
            throw new SubprocessExecutionException(e.getCause());
        }
    }

    @Override
    public ProcessResult<SO, SE> await() throws SubprocessException, InterruptedException {
        try {
            return future().get();
        } catch (ExecutionException e) {
            throw new SubprocessExecutionException(e.getCause());
        }
    }
}
