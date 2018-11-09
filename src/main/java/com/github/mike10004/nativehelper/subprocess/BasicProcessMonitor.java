package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.Subprocess.ProcessExecutionException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class BasicProcessMonitor<SO, SE> implements ProcessMonitor<SO, SE> {

    private final Process process;
    private final Future<ProcessResult<SO, SE>> future;
    private final ProcessTracker processTracker;

    BasicProcessMonitor(Process process, Future<ProcessResult<SO, SE>> future, ProcessTracker processTracker) {
        this.future = requireNonNull(future);
        this.process = requireNonNull(process);
        this.processTracker = requireNonNull(processTracker);
    }

    public Future<ProcessResult<SO, SE>> future() {
        return future;
    }

    public ProcessDestructor destructor() {
        return new BasicProcessDestructor(process, processTracker);
    }

    public Process process() {
        return process;
    }

    public ProcessTracker tracker() {
        return processTracker;
    }

    public static class ProcessExecutionInnerException extends ProcessExecutionException {
        public ProcessExecutionInnerException(Throwable cause) {
            super(cause);
        }
    }

    public ProcessResult<SO, SE> await(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, ProcessExecutionInnerException {
        try {
            boolean ok = process.waitFor(timeout, unit);
            if (!ok) {
                throw new TimeoutException("process.waitFor timeout elapsed");
            }
            return future().get();
        } catch (ExecutionException e) {
            throw new ProcessExecutionInnerException(e.getCause());
        }
    }

    public ProcessResult<SO, SE> await() throws ProcessException, InterruptedException {
        try {
            return future().get();
        } catch (ExecutionException e) {
            throw new ProcessExecutionInnerException(e.getCause());
        }
    }
}
