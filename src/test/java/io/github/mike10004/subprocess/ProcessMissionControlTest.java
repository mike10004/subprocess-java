package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.test.Tests;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public class ProcessMissionControlTest {

    private static final ProcessTracker PROCESS_TRACKER = new ShutdownHookProcessTracker();

    private static final StreamAttachmentSignal INACTIVE_SIGNAL = new StreamAttachmentSignal() {
        @Override
        public void notifyStreamsAttached() {
        }

        @Override
        public boolean await(long duration, TimeUnit unit) throws InterruptedException {
            return false;
        }
    };

    @Test
    public void launch() throws ExecutionException, InterruptedException {
        String expected = "hello";
        Subprocess subprocess = Subprocess.running("echo")
                .args(expected)
                .build();
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, PROCESS_TRACKER, INACTIVE_SIGNAL, createExecutorService());
        ByteBucket stdout = ByteBucket.create(), stderr = ByteBucket.create();
        PredefinedStreamControl endpoints = new PredefinedStreamControl(stdout, stderr, null);
        ProcessExecution<?, ?> execution = executor.launch(endpoints, exitCode -> ProcessResult.direct(exitCode, null, null));
        int exitCode = execution.getFuture().get().exitCode();
        assertEquals("exitcode", 0, exitCode);
        String actual = new String(stdout.dump(), US_ASCII);
        assertEquals("stdout", expected, actual.trim());
    }

    @Test
    public void launch_ProcessOutputControls_memory() throws Exception {
        String expected = "hello";
        Subprocess subprocess = Subprocess.running("echo")
                .args(expected)
                .build();
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, PROCESS_TRACKER, INACTIVE_SIGNAL, createExecutorService());
        @SuppressWarnings("unchecked")
        StreamContext<StreamContexts.BucketContext, StreamInput, StreamInput> ctrl = (StreamContext<StreamContexts.BucketContext, StreamInput, StreamInput>) StreamContexts.memoryByteSources(null);
        StreamControl outputcontext = ctrl.produceControl();
        ProcessExecution<StreamInput, StreamInput> execution = executor.launch(outputcontext, c -> {
                    StreamContent<StreamInput, StreamInput> xformed = ctrl.transform(c, (StreamContexts.BucketContext) outputcontext);
                    return ProcessResult.direct(c, xformed);
                });
        ProcessResult<StreamInput, StreamInput> result = execution.getFuture().get();
        assertEquals("exitcode", 0, result.exitCode());
        StreamInput stdout = result.content().stdout();
        String actual = new String(stdout.read(), US_ASCII);
        assertEquals("stdout", expected, actual.trim());
    }

    @Test
    public void readStdin() throws Exception {
        byte[] input = { 1, 2, 3, 4 };
        Subprocess subprocess = Tests.runningPythonFile("nht_length.py")
                .build();
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, PROCESS_TRACKER, INACTIVE_SIGNAL, createExecutorService());
        ByteBucket stdoutBucket = ByteBucket.create();
        PredefinedStreamControl endpoints = PredefinedStreamControl.builder()
                .stdin(StreamInput.wrap(input))
                .stdout(stdoutBucket)
                .build();
        ProcessExecution<Void, Void> execution = executor.launch(endpoints, c -> ProcessResult.direct(c, null, null));
        int exitCode = execution.getFuture().get(5, TimeUnit.SECONDS).exitCode();
        System.out.println(stdoutBucket);
        assertEquals("exitcode", 0, exitCode);
        String length = new String(stdoutBucket.dump(), US_ASCII);
        System.out.format("output: %s%n", length);
        assertEquals("length", String.valueOf(input.length), length);
    }

    private ExecutorService createExecutorService() {
        return ExecutorServices.newSingleThreadExecutorServiceFactory("ProcessMissionControlTest").get();
    }

    @Test(expected = ProcessMissionControl.ProcessStartException.class)
    public void startExceptionThrownIfExecutableNotFound() throws Exception {
        String executableName = UUID.randomUUID().toString();
        try (ScopedProcessTracker tracker = new ScopedProcessTracker()){
            Subprocess p = Subprocess.running(executableName).build();
            new BasicSubprocessLauncher(tracker).launch(p);
        }
    }

    @Test(expected = ProcessMissionControl.InvalidWorkingDirectoryException.class)
    public void workingDirectoryExceptionThrownIfDoesNotExist() throws Exception {
        File workingDir = new File(FileUtils.getUserDirectory(), UUID.randomUUID().toString());
        String executableName = UUID.randomUUID().toString();
        try (ScopedProcessTracker tracker = new ScopedProcessTracker()){
            Subprocess p = Subprocess.running(executableName)
                    .from(workingDir)
                    .build();
            new BasicSubprocessLauncher(tracker).launch(p);
        }
    }

}