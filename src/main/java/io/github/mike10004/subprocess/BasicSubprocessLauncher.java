package io.github.mike10004.subprocess;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class BasicSubprocessLauncher implements SubprocessLauncher {

    private final ProcessTracker processTracker;
    private final Supplier<? extends ExecutorService> launchExecutorServiceFactory;

    public BasicSubprocessLauncher(ProcessTracker processTracker) {
        launchExecutorServiceFactory = ExecutorServices.newSingleThreadExecutorServiceFactory("subprocess-launcher");
        this.processTracker = requireNonNull(processTracker, "processTracker");
    }

    @Override
    public <C extends StreamControl, SO, SE> ProcessMonitor<SO, SE> launch(Subprocess subprocess, StreamContext<C, SO, SE> streamContext) throws SubprocessException {
        C streamControl;
        try {
            streamControl = streamContext.produceControl();
        } catch (IOException e) {
            throw new SubprocessLaunchException("failed to produce output context", e);
        }
        // a one-time use executor service; it is shutdown immediately after exactly one task is submitted
        ExecutorService launchExecutorService = launchExecutorServiceFactory.get();
        ProcessMissionControl.Execution<SO, SE> execution = new ProcessMissionControl(subprocess, processTracker, launchExecutorService)
                .launch(streamControl, exitCode -> {
                    StreamContent<SO, SE> content = streamContext.transform(exitCode, streamControl);
                    return ProcessResult.direct(exitCode, content);
                });
        Future<ProcessResult<SO, SE>> fullResultFuture = execution.getFuture();
        launchExecutorService.shutdown(); // previously submitted tasks are executed
        ProcessMonitor<SO, SE> monitor = new BasicProcessMonitor<>(execution.getProcess(), fullResultFuture, processTracker);
        return monitor;
    }

}
