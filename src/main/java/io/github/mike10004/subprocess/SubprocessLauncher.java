package io.github.mike10004.subprocess;

/**
 * Interface of a service that launches subprocesses.
 */
public interface SubprocessLauncher {

    /**
     * Launches the subprocess defined by this instance and ignores all output.
     * Use {@link #launcher(ProcessTracker)} to build a launcher
     * and invoke {@link SubprocessLauncher#launch()} for a more fluent way of executing this method.
     * @return a process monitor
     * @throws SubprocessException
     */
    default ProcessMonitor<Void, Void> launch(Subprocess subprocess) throws SubprocessException {
        StreamContext<?, Void, Void> sinkhole = StreamContexts.sinkhole();
        return launch(subprocess, sinkhole);
    }

    /**
     * Launches the subprocess defined by this instance in the given input/output stream context.
     * Use {@link #launcher(ProcessTracker)} to build a launcher
     * and invoke {@link SubprocessLauncher#launch()} for a more fluent way of executing this method.
     * @param streamContext stream context
     * @param <C> stream control type
     * @param <SO> type of captured standard output content
     * @param <SE> type of captured standard error content
     * @return a process monitor
     * @throws SubprocessException if the process cannot be launched
     */
    <C extends StreamControl, SO, SE> ProcessMonitor<SO, SE> launch(Subprocess subprocess, StreamContext<C, SO, SE> streamContext) throws SubprocessException;

}
