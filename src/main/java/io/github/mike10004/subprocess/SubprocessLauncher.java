package io.github.mike10004.subprocess;

/**
 * Interface of a service that launches subprocesses.
 * Instead of using implementations of this interface directly, you can invoke
 * {@link Subprocess#launcher(SubprocessLauncher) Subprocess.launcher()} to
 * create a {@link SubprocessLaunchSupport} instance.
 */
public interface SubprocessLauncher {

    /**
     * Launches the given ignores all output.
     * @return a process monitor
     * @throws SubprocessException
     */
    default ProcessMonitor<Void, Void> launch(Subprocess subprocess) throws SubprocessException {
        StreamContext<?, Void, Void> sinkhole = StreamContexts.sinkhole();
        return launch(subprocess, sinkhole);
    }

    /**
     * Launches a subprocess in the given input/output stream context.
     * @param streamContext stream context
     * @param <C> stream control type
     * @param <SO> type of captured standard output content
     * @param <SE> type of captured standard error content
     * @return a process monitor
     * @throws SubprocessException if the process cannot be launched
     */
    <C extends StreamControl, SO, SE> ProcessMonitor<SO, SE> launch(Subprocess subprocess, StreamContext<C, SO, SE> streamContext) throws SubprocessException;

}
