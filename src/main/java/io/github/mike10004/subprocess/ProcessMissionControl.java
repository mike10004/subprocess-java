/*
 * A lot of code taken from Execute class in apache-ant and (c) Apache Software Foundation
 */
package io.github.mike10004.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class ProcessMissionControl {

    private static final Logger log = LoggerFactory.getLogger(ProcessMissionControl.class);

    private final ExecutorService terminationWaitingService;
    private final Subprocess program;
    private final ProcessTracker processTracker;

    public ProcessMissionControl(Subprocess program, ProcessTracker processTracker, ExecutorService terminationWaitingService) {
        this.program = requireNonNull(program);
        this.processTracker = requireNonNull(processTracker);
        this.terminationWaitingService = requireNonNull(terminationWaitingService);
    }

    public interface Execution<SO, SE> {
        Process getProcess();
        Future<ProcessResult<SO, SE>> getFuture();
    }

    public <SO, SE> Execution<SO, SE> launch(StreamControl streamControl, Function<? super Integer, ? extends ProcessResult<SO, SE>> resultTransform) {
        Process process = execute();
        Future<ProcessResult<SO, SE>> future = terminationWaitingService.submit(new Callable<ProcessResult<SO, SE>>(){
            @Override
            public ProcessResult<SO, SE> call() throws Exception {
                Integer exitCode = follow(process, streamControl);
                return resultTransform.apply(exitCode);
            }
        });
        return new Execution<SO, SE>() {
            @Override
            public Process getProcess() {
                return process;
            }

            @Override
            public Future<ProcessResult<SO, SE>> getFuture() {
                return future;
            }
        };
    }

    /**
     * Returns a copy of the command line, including executable and arguments.
     * @return the command line as a list
     */
    private List<String> getCommandLine() {
        return Stream.concat(Stream.of(program.executable()), program.arguments().stream()).collect(Collectors.toList());
    }

    private Process createProcess(List<String> cmdline) {
        ProcessBuilder pb = new ProcessBuilder()
                .command(cmdline)
                .redirectError(Redirect.PIPE)
                .redirectOutput(Redirect.PIPE)
                .redirectInput(Redirect.PIPE)
                .directory(program.workingDirectory());
        Map<String, String> pbenv = pb.environment();
        pbenv.putAll(program.environment());
        try {
            return pb.start();
        } catch (IOException e) {
            throw new ProcessStartException(e);
        }
    }

    static class ProcessStartException extends SubprocessLaunchException {
        public ProcessStartException(IOException cause) {
            super(cause);
        }
    }

    /**
     * Runs a process and returns its exit status.
     *
     * @return the exit status of the subprocess or null if the process did
     * @exception SubprocessException The exception is thrown, if launching
     *            of the subprocess failed.
     */
    @VisibleForTesting
    Process execute() {
        File workingDirectory = program.workingDirectory();
        if (!checkWorkingDirectory(workingDirectory)) {
            throw new InvalidWorkingDirectoryException(workingDirectory);
        }
        final Process process = createProcess(getCommandLine());
        processTracker.add(process);
        return process;
    }

    private static boolean checkWorkingDirectory(@Nullable File workingDirectory) {
        return workingDirectory == null || workingDirectory.isDirectory();
    }

    static class InvalidWorkingDirectoryException extends SubprocessLaunchException {

        public InvalidWorkingDirectoryException(File workingDirectory) {
            super("specified working directory " + workingDirectory + " is not a directory; is file? " + workingDirectory.isFile());
        }

    }

    private static class MaybeNullResource<T extends java.io.Closeable> implements java.io.Closeable {
        @Nullable
        public final T resource;

        private MaybeNullResource(@Nullable T closeable) {
            this.resource = closeable;
        }

        @Override
        public void close() throws IOException {
            if (resource != null) {
                resource.close();
            }
        }

        public static <T extends Closeable> MaybeNullResource<T> of(T stream) {
            return new MaybeNullResource<>(stream);
        }
    }

    @VisibleForTesting
    @Nullable
    Integer follow(Process process, StreamControl outputContext) throws IOException {
        boolean terminated = false;
        @Nullable Integer exitVal;
        OutputStream processStdin = null;
        InputStream processStdout = null, processStderr = null;
        try (MaybeNullResource<InputStream> inResource = MaybeNullResource.of(outputContext.openStdinSource());
            OutputStream stdoutDestination = outputContext.openStdoutSink();
            OutputStream stderrDestination = outputContext.openStderrSink()) {
            StreamConduit conduit = new StreamConduit(stdoutDestination, stderrDestination, inResource.resource);
            processStdin = process.getOutputStream();
            processStdout = process.getInputStream();
            processStderr = process.getErrorStream();
            try (Closeable ignore = conduit.connect(processStdin, processStdout, processStderr)) {
                exitVal = waitFor(process);
                if (exitVal != null) {
                    terminated = true;
                }
            }
        } finally {
            if (!terminated) {
                destroy(process);
            }
            processTracker.remove(process);
            Streams.closeAllAndIgnoreErrors(processStdin, processStdout, processStderr);
        }
        if (exitVal == null) {
            throw new IllegalProcessStateException("no way to wait for process; probably interrupted in ProcessMissionControl.waitFor");
        }
        return exitVal;
    }

    private static class IllegalProcessStateException extends IllegalStateException {
        public IllegalProcessStateException(String msg) {
            super(msg);
        }
    }

    private void destroy(Process process) {
        boolean terminatedNaturally = false;
        try {
            terminatedNaturally = process.waitFor(0, TimeUnit.MILLISECONDS);
        } catch(InterruptedException e) {
            log.error("interrupted while waiting with timeout 0", e);
            throw new IllegalStateException("BUG: interrupted exception shouldn't happen because timeout length is zero", e);
        } finally {
            if (!terminatedNaturally && process.isAlive()) {
                process.destroy();
            }
        }

    }

    /**
     * Wait for a given process.
     * @param process the process to wait for
     */
    @Nullable
    private Integer waitFor(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            log.info("interrupted in Process.waitFor");
            return null;
        }
    }

}
