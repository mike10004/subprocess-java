package io.github.mike10004.subprocess;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Service class that implements basic subprocess launching.
 */
public class BasicSubprocessLauncher implements SubprocessLauncher {

    private static final String THREAD_POOL_NAME = "subprocess-launcher";

    private final ProcessTracker processTracker;
    private final Supplier<? extends ExecutorService> launchExecutorServiceFactory;

    /**
     * Constructs an instance with the given process tracker and a new executor
     * service factory that constructs a new single thread executor.
     * @param processTracker the process tracker
     * @see java.util.concurrent.Executors#newSingleThreadExecutor
     */
    public BasicSubprocessLauncher(ProcessTracker processTracker) {
        this(processTracker, ExecutorServices.newSingleThreadExecutorServiceFactory(THREAD_POOL_NAME));
    }

    /**
     * Constructs an instance with the given process tracker and executor service factory.
     * The factory provides the executor service to which the task of blocking until
     * the process completes is performed. After that task has completed, the executor
     * service is shut down and is not reused.
     * @param processTracker process tracker
     * @param launchExecutorServiceFactory executor service factory
     */
    protected BasicSubprocessLauncher(ProcessTracker processTracker, Supplier<? extends ExecutorService> launchExecutorServiceFactory) {
        this.launchExecutorServiceFactory = requireNonNull(launchExecutorServiceFactory, "launchExecutorServiceFactory");
        this.processTracker = requireNonNull(processTracker, "processTracker");
    }

    /**
     * Gets the default thread pool name. If an instance of this class is constructed
     * with the the default executor service, then this string is used as a prefix
     * for the thread names that the executor service creates.
     * @return the default thread pool name
     */
    public static String getDefaultThreadPoolName() {
        return THREAD_POOL_NAME;
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
        ProcessExecution<SO, SE> execution = new ProcessMissionControl(subprocess, processTracker, launchExecutorService)
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
