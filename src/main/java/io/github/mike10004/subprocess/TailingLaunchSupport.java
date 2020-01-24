package io.github.mike10004.subprocess;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Service class that provides support for tailing process output streams.
 * To tail an output stream is to consume it one line at a time.
 */
public class TailingLaunchSupport extends NonCapturingLaunchSupport {

    private final StreamTailerContext lineConsumerContext;
    private final ExecutorService tailThreadExecutorService;
    private final StreamTailer tailer;

    /**
     * Constructs an instance of the class
     * @param subprocess subprocess
     * @param launcher launcher
     * @param tailContext stream context
     * @param tailThreadExecutorService service to execute the threads that tail process output
     * @param tailer stream tailer
     */
    public TailingLaunchSupport(Subprocess subprocess, SubprocessLauncher launcher, StreamTailerContext tailContext, ExecutorService tailThreadExecutorService, StreamTailer tailer) {
        super(subprocess, launcher, tailContext);
        this.lineConsumerContext = requireNonNull(tailContext);
        this.tailer = requireNonNull(tailer);
        this.tailThreadExecutorService = requireNonNull(tailThreadExecutorService);
    }

    private static long saturatedMilliseconds(Duration duration) {
        try {
            return duration.toMillis();
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Launches the process, relaying output to this instance's stream tailer.
     * @return a process monitor
     * @see SubprocessLaunchSupport#launch()
     */
    @Override
    public ProcessMonitor<Void, Void> launch() {
        ProcessMonitor<Void, Void> monitor = super.launch();
        boolean streamsAttached = false;
        InterruptedException interruption = null;
        long saturatedMillisTimeout = saturatedMilliseconds(tailer.streamAttachTimeout());
        long startMillisEpoch = System.currentTimeMillis(), endMillisEpoch;
        try {
            streamsAttached = monitor.awaitStreamsAttached(saturatedMillisTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            interruption = e;
        } finally {
            endMillisEpoch = System.currentTimeMillis();
        }
        StreamTailer.StreamAttachmentListener attachmentListener = tailer.streamAttachmentListener();
        Duration elapsed = Duration.ofMillis(endMillisEpoch - startMillisEpoch);
        attachmentListener.streamAttachmentWaitFinished(streamsAttached, elapsed, interruption);
        if (streamsAttached) {
            lineConsumerContext.startRelaying(tailThreadExecutorService, tailer.charset(), tailer::acceptStdout, tailer::acceptStderr);
        }
        return monitor;
    }
}
