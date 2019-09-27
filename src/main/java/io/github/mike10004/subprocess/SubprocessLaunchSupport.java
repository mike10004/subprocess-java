package io.github.mike10004.subprocess;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Support class that provides a fluent interface to launch a process. This is essentially
 * a builder of the stream context. Instances of this class are immutable,
 * so the fluent API must be used.
 * @param <SO> standard output capture type
 * @param <SE> standard error capture type
 */
public class SubprocessLaunchSupport<SO, SE> {

    private final Subprocess subprocess;
    private final SubprocessLauncher launcher;
    protected final StreamContext<?, SO, SE> streamContext;

    @VisibleForTesting
    SubprocessLaunchSupport(Subprocess subprocess, SubprocessLauncher launcher, StreamContext<?, SO, SE> streamContext) {
        this.subprocess = requireNonNull(subprocess, "subprocess");
        this.launcher = requireNonNull(launcher, "launcher");
        this.streamContext = requireNonNull(streamContext, "streamContext");
    }

    public ProcessMonitor<SO, SE> launch() {
        return launcher.launch(subprocess, streamContext);
    }

    /**
     * Return a new uniform launcher that uses the given stream context.
     * @param streamContext the stream context of the new launcher
     * @param <S> type of captured standard output and standard error
     * @return a new launcher instance
     */
    public <S> UniformSubprocessLaunchSupport<S> output(UniformStreamContext<?, S> streamContext) {
        return uniformOutput(streamContext);
    }

    /**
     * Return a new uniform launcher that uses the given stream context.
     * @param streamContext the stream context of the new launcher
     * @param <S> type of captured standard output and standard error
     * @return a new launcher instance
     */
    public <S> UniformSubprocessLaunchSupport<S> uniformOutput(StreamContext<?, S, S> streamContext) {
        return new UniformSubprocessLaunchSupport<>(subprocess, launcher, streamContext);
    }

    /**
     * Return a new launcher that uses the given stream context.
     * @param streamContext the stream context of the new launcher
     * @param <SO2> type of standard output content captured by the new launcher
     * @param <SE2> type of standard error content captured by the new launcher
     * @return a new launcher instance
     */
    public <SO2, SE2> SubprocessLaunchSupport<SO2, SE2> output(StreamContext<?, SO2, SE2> streamContext) {
        return new SubprocessLaunchSupport<>(subprocess, launcher, streamContext);
    }

    /**
     * Returns a new launcher that maps this launcher's output.
     * @param stdoutMap function that maps standard output content
     * @param stderrMap function that maps standard error content
     * @param <SO2> type of mapped standard output content
     * @param <SE2> type of mapped standard error content
     * @return a new launcher instance
     */
    public <SO2, SE2> SubprocessLaunchSupport<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        return output(streamContext.map(stdoutMap, stderrMap));
    }

    /**
     * Returns a new launcher that captures the content of process standard output
     * and error streams in memory as strings.
     * The specified characer encoding is used to decode the bytes collected from the
     * process standard output and standard error streams. In many cases an external
     * process will write text output in the platform-default encoding, which can be
     * obtained by {@link Charset#defaultCharset()}. Use a different argument if you
     * know the encoding with which the program writes its output.
     * @param charset encoding of bytes on the process standard output and error streams
     * @return a new launch support instance
     * @see #outputStrings(Charset, StreamInput)
     */
    public UniformSubprocessLaunchSupport<String> outputStrings(Charset charset) {
        requireNonNull(charset, "charset");
        return outputStrings(charset, null);
    }

    /**
     * Returns a new launcher that captures the content of process standard output and error as strings.
     * The specified characer encoding is used to decode the bytes collected from the
     * process standard output and standard error streams.
     * @param charset encoding of bytes on the process standard output and error streams
     * @param stdin source providing bytes to be written on process standard input stream; may be null
     * @return a new launch support instance
     */
    public UniformSubprocessLaunchSupport<String> outputStrings(Charset charset, @Nullable StreamInput stdin) {
        requireNonNull(charset, "charset");
        return output(StreamContexts.strings(charset, stdin));
    }

    /**
     * Returns a new launcher that captures the content of the process standard
     * output and error streams in memory as byte arrays.
     * @return a new launch support instance
     * @see #outputInMemory(StreamInput)
     */
    public UniformSubprocessLaunchSupport<byte[]> outputInMemory() {
        return outputInMemory(null);
    }

    /**
     * Returns a new launcher that captures process standard output and error as byte arrays.
     * @param stdin source providing bytes to be written on process standard input stream; may be null
     * @return a new launch support instance
     */
    public UniformSubprocessLaunchSupport<byte[]> outputInMemory(@Nullable StreamInput stdin) {
        UniformStreamContext<?, byte[]> m = StreamContexts.byteArrays(stdin);
        return output(m);
    }

    /**
     * Returns a new launcher that pipes process output to the JVM standard output and errors streams and
     * pipes input from the JVM standard input stream to the process standard input stream.
     * @return a new launch support instance
     */
    @SuppressWarnings("unused")
    public SubprocessLaunchSupport<Void, Void> inheritAllStreams() {
        return output(StreamContexts.inheritAll());
    }

    /**
     * Returns a new launcher that pipes process output to the JVM standard output and errors streams
     * but does not write anything on the process standard input stream.
     * @return a new launch support instance
     */
    public SubprocessLaunchSupport<Void, Void> inheritOutputStreams() {
        return output(StreamContexts.inheritOutputs());
    }

    /**
     * Returns a new launcher that captures the process standard output and error content
     * in files.
     * @param stdoutFile the file to which standard output content is to be written
     * @param stderrFile the file to which standard error content is to be written
     * @param stdin source providing bytes to be written on process standard input stream; may be null
     * @return a new launch support instance
     */
    public UniformSubprocessLaunchSupport<File> outputFiles(File stdoutFile, File stderrFile, @Nullable StreamInput stdin) {
        return output(StreamContexts.outputFiles(stdoutFile, stderrFile, stdin));
    }

    /**
     * Returns a new launcher that captures the process standard output and error content
     * in files.
     * @param stdoutFile the file to which standard output content is to be written
     * @param stderrFile the file to which standard error content is to be written
     * @return a new launch support instance
     * @see #outputFiles(File, File, StreamInput)
     */
    public UniformSubprocessLaunchSupport<File> outputFiles(File stdoutFile, File stderrFile) {
        return outputFiles(stdoutFile, stderrFile, null);
    }

    /**
     * Returns a new launcher that captures the process standard output and error content
     * in new, uniquely-named files created in the given directory.
     * @param directory pathname of a existing directory in which files are to be created
     * @return a new launch support instance
     */
    public UniformSubprocessLaunchSupport<File> outputTempFiles(Path directory) {
        return outputTempFiles(directory, null);
    }

    /**
     * Returns a new launcher that captures the process standard output and error content
     * in new, uniquely-named files created in the given directory.
     * @param directory pathname of a existing directory in which files are to be created
     * @param stdin source providing bytes to be written on process standard input stream; may be null
     * @return a new launch support instance
     */
    public UniformSubprocessLaunchSupport<File> outputTempFiles(Path directory, @Nullable StreamInput stdin) {
        return output(StreamContexts.outputTempFiles(directory, stdin));
    }

}
