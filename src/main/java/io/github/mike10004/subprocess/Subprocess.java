package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.StreamContext.UniformStreamContext;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.mike10004.subprocess.Preconditions.checkArgument;
import static io.github.mike10004.subprocess.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Class that represents a subprocess to be executed. Instances of this class
 * are immutable and may be reused. This API adheres to an asynchronous model,
 * so after you launch a process, you receive a {@link ProcessMonitor monitor}
 * instance that allows you to wait for the result on the current thread or
 * attach a listener notified when the process terminates.
 *
 * <p>To launch a process and ignore the output:</p>
 * <pre>
 * {@code
 *     ProcessMonitor<?, ?> monitor = Subprocess.running("true").build()
 *             .launcher(ProcessTracker.create())
 *             .launch();
 *     ProcessResult<?, ?> result = monitor.await();
 *     System.out.println("exit with status " + result.exitCode()); // exit with status 0
 * }
 * </pre>
 *
 * <p>To launch a process and capture the output as strings:</p>
 * <pre>
 * {@code
 *     ProcessMonitor<String, String> monitor = Subprocess.running("echo")
 *             .arg("hello, world")
 *             .build()
 *             .launcher(ProcessTracker.create())
 *             .outputStrings(Charset.defaultCharset())
 *             .launch();
 *     ProcessResult<String, String> result = monitor.await();
 *     System.out.println(result.content().stdout()); // hello, world
 * }
 * </pre>
 *
 * <p>To launch a process and capture the output in files:</p>
 * <pre>
 * {@code
 *     ProcessMonitor<File, File> monitor = Subprocess.running("echo")
 *             .arg("this is in a file")
 *             .build()
 *             .launcher(ProcessTracker.create())
 *             .outputTempFiles(new File(System.getProperty("java.io.tmpdir")).toPath())
 *             .launch();
 *     ProcessResult<File, File> result = monitor.await();
 *     System.out.println("printed:");
 *     java.nio.file.Files.copy(result.content().stdout().toPath(), System.out); // this is in a file
 * }
 * </pre>
 *
 * <p>To launch a process and send it data on standard input:</p>
 * <pre>
 * {@code
 *     ByteSource input = Files.asByteSource(new File("/etc/passwd"));
 *     ProcessMonitor<String, String> monitor = Subprocess.running("grep")
 *             .arg("root")
 *             .build()
 *             .launcher(ProcessTracker.create())
 *             .outputStrings(Charset.defaultCharset(), input)
 *             .launch();
 *     ProcessResult<String, String> result = monitor.await();
 *     System.out.println("printed " + result.content().stdout()); // root:x:0:0:root:/root:/bin/bash
 * }
 * </pre>
 *
 * @since 7.1.0
 */
public class Subprocess {
    
    private final String executable;
    /**
     * Immutable list of arguments. List does not include executable.
     */
    private final List<String> arguments;
    @Nullable
    private final File workingDirectory;
    /**
     * Immutable map of environment variables that are to supplement the inherited environment.
     */
    private final Map<String, String> environment;
    private final Supplier<? extends ExecutorService> launchExecutorServiceFactory;

    protected Subprocess(String executable, @Nullable File workingDirectory, Map<String, String> environment, List<String> arguments) {
        this.executable = requireNonNull(executable, "executable");
        this.workingDirectory = workingDirectory;
        this.arguments = Defensive.copyOf(arguments);
        this.environment = Defensive.copyOf(environment);
        launchExecutorServiceFactory = ExecutorServices.newSingleThreadExecutorServiceFactory("subprocess-launch");
    }

    /**
     * Launches a subprocess. Use {@link #launcher(ProcessTracker)} to build a launcher
     * and invoke {@link Launcher#launch()} for a more fluent way of executing this method.
     * @param processTracker process tracker to use
     * @param streamContext stream context
     * @param <C> stream control type
     * @param <SO> type of captured standard output content
     * @param <SE> type of captured standard error content
     * @return a process monitor
     * @throws ProcessException if the process cannot be launched
     */
    public <C extends StreamControl, SO, SE> ProcessMonitor<SO, SE> launch(ProcessTracker processTracker, StreamContext<C, SO, SE> streamContext) throws ProcessException {
        C streamControl;
        try {
            streamControl = streamContext.produceControl();
        } catch (IOException e) {
            throw new ProcessLaunchException("failed to produce output context", e);
        }
        // a one-time use executor service; it is shutdown immediately after exactly one task is submitted
        ExecutorService launchExecutorService = launchExecutorServiceFactory.get();
        ProcessMissionControl.Execution<SO, SE> execution = new ProcessMissionControl(this, processTracker, launchExecutorService)
                .launch(streamControl, exitCode -> {
                    StreamContent<SO, SE> content = streamContext.transform(exitCode, streamControl);
                    return ProcessResult.direct(exitCode, content);
                });
        Future<ProcessResult<SO, SE>> fullResultFuture = execution.getFuture();
        launchExecutorService.shutdown(); // previously submitted tasks are executed
        ProcessMonitor<SO, SE> monitor = new BasicProcessMonitor<>(execution.getProcess(), fullResultFuture, processTracker);
        return monitor;
    }

    @SuppressWarnings("unused")
    public static class ProcessExecutionException extends ProcessException {
        public ProcessExecutionException(String message) {
            super(message);
        }

        public ProcessExecutionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProcessExecutionException(Throwable cause) {
            super(cause);
        }
    }


    /**
     * Class that represents a builder of program instances. Create a builder
     * instance with {@link Subprocess#running(String) }.
     * @see Subprocess
     */
    @NotThreadSafe
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        
        protected final String executable;
        protected File workingDirectory;
        protected final List<String> arguments;
        protected final Map<String, String> environment;

        /**
         * Constructs a builder instance.
         * @param executable the executable name or pathname of the executable file
         */
        protected Builder(String executable) {
            this.executable = Preconditions.checkNotNull(executable);
            Preconditions.checkArgument(!executable.isEmpty(), "executable must be non-empty string");
            arguments = new ArrayList<>();
            environment = new LinkedHashMap<>();
        }
        
        /**
         * Launch the process from this working directory.
         * @param workingDirectory the directory
         * @return this builder instance
         */
        public Builder from(File workingDirectory) {
            this.workingDirectory = Preconditions.checkNotNull(workingDirectory);
            return this;
        }

        /**
         * Set variables in the environment of the process to be executed.
         * Each entry in the argument map is put into this builder's environment map.
         * Existing entries are not cleared; use {@link #clearEnv()} for that.
         * @param environment map of variables to set
         * @return this builder instance
         */
        public Builder env(Map<String, String> environment) {
            this.environment.putAll(environment);
            return this;
        }

        /**
         * Set variable in the environment of the process to be executed.
         * @param name variable name
         * @param value variable value
         * @return this builder instance
         */
        public Builder env(String name, String value) {
            environment.put(name, Preconditions.checkNotNull(value, "value must be non-null"));
            return this;
        }

        /**
         * Clears this builder's environment map.
         * @return this instance
         */
        @SuppressWarnings("unused")
        public Builder clearEnv() {
            this.environment.clear();
            return this;
        }

        /**
         * Clears the argument list of this builder.
         * @return this builder instance
         */
        @SuppressWarnings("unused")
        public Builder clearArgs() {
            this.arguments.clear();
            return this;
        }
        
        /**
         * Appends an argument to the argument list of this builder.
         * @param argument the argument
         * @return this builder instance
         */
        public Builder arg(String argument) {
            this.arguments.add(Preconditions.checkNotNull(argument));
            return this;
        }

        /**
         * Appends arguments to the argument list of this builder.
         * @param firstArgument the first argument to append
         * @param otherArguments the other arguments to append
         * @return this builder instance
         * @see #args(Iterable)
         */        
        public Builder args(String firstArgument, String...otherArguments) {
            return args(Stream.concat(Stream.of(firstArgument), Arrays.stream(otherArguments)));
        }

        private Builder args(Stream<String> stream) {
            stream.forEach(this.arguments::add);
            return this;
        }

        /**
         * Appends arguments to the argument list of this builder.
         * @param arguments command line arguments
         * @return this builder instance
         */
        public Builder args(Iterable<String> arguments) {
            Defensive.requireAllNotNull(arguments, "all arguments must be non-null");
            return args(StreamSupport.stream(arguments.spliterator(), false));
        }

        public Subprocess build() {
            return new Subprocess(executable, workingDirectory, environment, arguments);
        }

    }

    /**
     * Creates a new launcher in the given process context. The launcher
     * created does not specify that output is to be captured. Use the
     * {@code Launcher} methods to specify how input is to be sent to the process and how
     * output is to be captured.
     * @param processTracker the process context
     * @return the launcher
     */
    public Launcher<Void, Void> launcher(ProcessTracker processTracker) {
        return toSinkhole(processTracker);
    }

    private Launcher<Void, Void> toSinkhole(ProcessTracker processTracker) {
        return new Launcher<Void, Void>(processTracker, StreamContexts.sinkhole()){};
    }

    /**
     * Helper class that retains references to some dependencies so that you can
     * use a builder-style pattern to launch a process. Instances of this class are immutable
     * and methods that have return type {@code Launcher} return new instances.
     * @param <SO> standard output capture type
     * @param <SE> standard error capture type
     */
    public abstract class Launcher<SO, SE> {

        private final ProcessTracker processTracker;
        protected final StreamContext<?, SO, SE> streamContext;

        private Launcher(ProcessTracker processTracker, StreamContext<?, SO, SE> streamContext) {
            this.processTracker = requireNonNull(processTracker);
            this.streamContext = requireNonNull(streamContext);
        }

        /**
         * Return a new uniform launcher that uses the given stream context.
         * @param streamContext the stream context of the new launcher
         * @param <S> type of captured standard output and standard error
         * @return a new launcher instance
         */
        public <S> UniformLauncher<S> output(UniformStreamContext<?, S> streamContext) {
            return uniformOutput(streamContext);
        }

        /**
         * Return a new uniform launcher that uses the given stream context.
         * @param streamContext the stream context of the new launcher
         * @param <S> type of captured standard output and standard error
         * @return a new launcher instance
         */
        public <S> UniformLauncher<S> uniformOutput(StreamContext<?, S, S> streamContext) {
            return new UniformLauncher<S>(processTracker, streamContext) {};
        }

        /**
         * Return a new launcher that uses the given stream context.
         * @param streamContext the stream context of the new launcher
         * @param <SO2> type of standard output content captured by the new launcher
         * @param <SE2> type of standard error content captured by the new launcher
         * @return a new launcher instance
         */
        public <SO2, SE2> Launcher<SO2, SE2> output(StreamContext<?, SO2, SE2> streamContext) {
            return new Launcher<SO2, SE2>(processTracker, streamContext) {};
        }

        /**
         * Launches the process.
         * @return the process monitor
         * @throws ProcessException  if there is an error that prevents the process from being launched
         */
        public ProcessMonitor<SO, SE> launch() throws ProcessException {
            return Subprocess.this.launch(processTracker, streamContext);
        }

        /**
         * Returns a new launcher that maps this launcher's output.
         * @param stdoutMap function that maps standard output content
         * @param stderrMap function that maps standard error content
         * @param <SO2> type of mapped standard output content
         * @param <SE2> type of mapped standard error content
         * @return a new launcher instance
         */
        public <SO2, SE2> Launcher<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
            return output(streamContext.map(stdoutMap, stderrMap));
        }

        /**
         * Returns a new launcher that captures process standard output and error as strings.
         * The specified characer encoding is used to decode the bytes collected from the
         * process standard output and standard error streams.
         * @param charset encoding of bytes on the process standard output and error streams
         * @return a new launcher instance
         * @see #outputStrings(Charset, StreamInput)
         */
        public UniformLauncher<String> outputStrings(Charset charset) {
            requireNonNull(charset, "charset");
            return outputStrings(charset, null);
        }

        /**
         * Returns a new launcher that captures process standard output and error as strings.
         * The specified characer encoding is used to decode the bytes collected from the
         * process standard output and standard error streams.
         * @param charset encoding of bytes on the process standard output and error streams
         * @param stdin source providing bytes to be written on process standard input stream; may be null
         * @return a new launcher instance
         */
        public UniformLauncher<String> outputStrings(Charset charset, @Nullable StreamInput stdin) {
            requireNonNull(charset, "charset");
            return output(StreamContexts.strings(charset, stdin));
        }

        /**
         * Returns a new launcher that captures process standard output and error as byte arrays.
         * @return a new launcher instance
         * @see #outputInMemory(StreamInput)
         */
        @SuppressWarnings("unused")
        public UniformLauncher<byte[]> outputInMemory() {
            return outputInMemory(null);
        }

        /**
         * Returns a new launcher that captures process standard output and error as byte arrays.
         * @param stdin source providing bytes to be written on process standard input stream; may be null
         * @return a new launcher instance
         */
        public UniformLauncher<byte[]> outputInMemory(@Nullable StreamInput stdin) {
            UniformStreamContext<?, byte[]> m = StreamContexts.byteArrays(stdin);
            return output(m);
        }

        /**
         * Returns a new launcher that pipes process output to the JVM standard output and errors streams and
         * pipes input from the JVM standard input stream to the process standard input stream.
         * @return a new launcher instance
         */
        @SuppressWarnings("unused")
        public Launcher<Void, Void> inheritAllStreams() {
            return output(StreamContexts.inheritAll());
        }

        /**
         * Returns a new launcher that pipes process output to the JVM standard output and errors streams
         * but does not write anything on the process standard input stream.
         * @return a new launcher instance
         */
        public Launcher<Void, Void> inheritOutputStreams() {
            return output(StreamContexts.inheritOutputs());
        }

        /**
         * Returns a new launcher that captures the process standard output and error content
         * in files.
         * @param stdoutFile the file to which standard output content is to be written
         * @param stderrFile the file to which standard error content is to be written
         * @param stdin source providing bytes to be written on process standard input stream; may be null
         * @return a new launcher instance
         */
        public UniformLauncher<File> outputFiles(File stdoutFile, File stderrFile, @Nullable StreamInput stdin) {
            return output(StreamContexts.outputFiles(stdoutFile, stderrFile, stdin));
        }

        /**
         * Returns a new launcher that captures the process standard output and error content
         * in files.
         * @param stdoutFile the file to which standard output content is to be written
         * @param stderrFile the file to which standard error content is to be written
         * @return a new launcher instance
         * @see #outputFiles(File, File, StreamInput)
         */
        public UniformLauncher<File> outputFiles(File stdoutFile, File stderrFile) {
            return outputFiles(stdoutFile, stderrFile, null);
        }

        /**
         * Returns a new launcher that captures the process standard output and error content
         * in new, uniquely-named files created in the given directory.
         * @param directory pathname of a existing directory in which files are to be created
         * @return a new launcher instance
         */
        public UniformLauncher<File> outputTempFiles(Path directory) {
            return outputTempFiles(directory, null);
        }

        /**
         * Returns a new launcher that captures the process standard output and error content
         * in new, uniquely-named files created in the given directory.
         * @param directory pathname of a existing directory in which files are to be created
         * @param stdin source providing bytes to be written on process standard input stream; may be null
         * @return a new launcher instance
         */
        public UniformLauncher<File> outputTempFiles(Path directory, @Nullable StreamInput stdin) {
            return output(StreamContexts.outputTempFiles(directory, stdin));
        }
    }

    /**
     * Class that represents a launcher using a uniform output control.
     * @param <S> type of captured standard output and standard error content
     */
    public abstract class UniformLauncher<S> extends Launcher<S, S> {

        private UniformLauncher(ProcessTracker processTracker, StreamContext<?, S, S> streamContext) {
            super(processTracker, streamContext);
        }

        /**
         * Returns a new launcher that maps captured standard output and standard error
         * content to a different type.
         * @param mapper map function
         * @param <T> destination type
         * @return a new launcher instance
         */
        public <T> UniformLauncher<T> map(Function<? super S, T> mapper) {
            UniformStreamContext<?, S> u = UniformStreamContext.wrap(this.streamContext);
            UniformStreamContext<?, T> t = u.map(mapper);
            return uniformOutput(t);
        }

    }

    /**
     * Constructs a builder instance that will produce a program that 
     * launches the given executable. Checks that a file exists at the given pathname
     * and that it is executable by the operating system.
     * @param executable the executable file
     * @return a builder instance
     * @throws IllegalArgumentException if {@link File#canExecute() } is false
     */
    public static Builder running(File executable) {
        Preconditions.checkArgument(executable.isFile(), "file not found: %s", executable);
        Preconditions.checkArgument(executable.canExecute(), "executable.canExecute");
        return running(executable.getPath());
    }
    
    /**
     * Constructs a builder instance that will produce a program that 
     * launches an executable corresponding to the argument string.
     * @param executable the name of an executable or a path to a file
     * @return a builder instance
     */
    public static Builder running(String executable) {
        return new Builder(executable);
    }

    /**
     * Gets the executable to be executed.
     * @return the executable
     */
    @SuppressWarnings("unused")
    public String executable() {
        return executable;
    }

    /**
     * Gets the arguments to be provided to the process.
     * @return the arguments
     */
    @SuppressWarnings("unused")
    public List<String> arguments() {
        return arguments;
    }

    /**
     * Gets the working directory of the process. Null means the working
     * directory of the JVM.
     * @return the working directory pathname
     */
    @Nullable
    protected File workingDirectory() {
        return workingDirectory;
    }

    /**
     * Gets the immutable map of environment variables to override or supplement
     * the process environment.
     * @return the environment
     */
    @SuppressWarnings("unused")
    public Map<String, String> environment() {
        return environment;
    }

    @Override
    public String toString() {
        return "Subprocess{" +
                "executable='" + executable + '\'' +
                ", arguments=" + StringUtils.abbreviateMiddle(String.valueOf(arguments), "...", 64) +
                ", workingDirectory=" + workingDirectory +
                ", environment=" + StringUtils.abbreviateMiddle(String.valueOf(environment), "...", 64) +
                '}';
    }

}
