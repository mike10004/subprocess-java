package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.test.Tests;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public class ProcessMissionControlTest {

    private static final ProcessTracker CONTEXT = ProcessTracker.create();

    @Test
    public void launch() throws ExecutionException, InterruptedException {
        String expected = "hello";
        Subprocess subprocess = Subprocess.running("echo")
                .args(expected)
                .build();
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, CONTEXT, createExecutorService());
        ByteBucket stdout = ByteBucket.create(), stderr = ByteBucket.create();
        PredefinedStreamControl endpoints = new PredefinedStreamControl(stdout, stderr, null);
        ProcessMissionControl.Execution<?, ?> execution = executor.launch(endpoints, exitCode -> ProcessResult.direct(exitCode, null, null));
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
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, CONTEXT, createExecutorService());
        @SuppressWarnings("unchecked")
        StreamContext<StreamContexts.BucketContext, StreamInput, StreamInput> ctrl = (StreamContext<StreamContexts.BucketContext, StreamInput, StreamInput>) StreamContexts.memoryByteSources(null);
        StreamControl outputcontext = ctrl.produceControl();
        ProcessMissionControl.Execution<StreamInput, StreamInput> execution = executor.launch(outputcontext, c -> {
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
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, CONTEXT, createExecutorService());
        ByteBucket stdoutBucket = ByteBucket.create();
        PredefinedStreamControl endpoints = PredefinedStreamControl.builder()
                .stdin(StreamInput.wrap(input))
                .stdout(stdoutBucket)
                .build();
        ProcessMissionControl.Execution<Void, Void> execution = executor.launch(endpoints, c -> ProcessResult.direct(c, null, null));
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
}