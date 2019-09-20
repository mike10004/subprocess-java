package io.github.mike10004.subprocess;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * Value class that represents a subprocess to be executed. Instances of this class
 * are immutable and may be reused. This API adheres to an asynchronous model,
 * so after you launch a process, you receive a {@link ProcessMonitor monitor}
 * instance that provides methods to block on the current thread or obtain
 * a {@link Future} result.
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
 * @since 0.1
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

    protected Subprocess(String executable, @Nullable File workingDirectory, Map<String, String> environment, List<String> arguments) {
        this.executable = requireNonNull(executable, "executable");
        this.workingDirectory = workingDirectory;
        this.arguments = Defensive.immutableCopyOf(arguments);
        this.environment = Defensive.immutableCopyOf(environment);
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
     * @param launcher the launcher
     * @return a launch platform
     */
    public <SO, SE> SubprocessLaunchSupport<SO, SE> launcher(SubprocessLauncher launcher, StreamContext<?, SO, SE> streamContext) {
        return new SubprocessLaunchSupport<>(Subprocess.this, launcher, streamContext);
    }

    /**
     * Creates a new launch support that ignores process output.
     * @param launcher the launcher
     * @return a new launch support instance
     * @see #launcher(SubprocessLauncher, StreamContext)
     */
    public SubprocessLaunchSupport<Void, Void> launcher(SubprocessLauncher launcher) {
        return launcher(launcher, StreamContexts.sinkhole());
    }

    /**
     * Invokes {@link #launcher(SubprocessLauncher)} with a newly constructed
     * {@link BasicSubprocessLauncher} instance.
     * @param processTracker a process tracker
     */
    public SubprocessLaunchSupport<Void, Void> launcher(ProcessTracker processTracker) {
        return launcher(new BasicSubprocessLauncher(processTracker));
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
