package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.test.Tests;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.junit.AssumptionViolatedException;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TailingLaunchSupportTest extends SubprocessTestBase {

    public TailingLaunchSupportTest(int trial) {
        super(trial);
    }

    @Test
    public void testLaunch() throws Exception {
        LaunchSupportConfigurator configurator = new LaunchSupportConfigurator() {
            @Override
            public TailingLaunchSupport configure(SubprocessLaunchSupport launchSupport, ExecutorService relayExecutorService, StreamTailer tailer) {
                return launchSupport.tailing(relayExecutorService, tailer);
            }
        };
        String pyfile = "nht_stereo.py";
        List<String> args = Arrays.asList("one", "two", "three", "four", "five");
        List<String> stdout = Arrays.asList("one", "three", "five");
        List<String> stderr = Arrays.asList("two", "four");
        Runnable postLaunchAction = () -> {};
        testLaunchAndTail(pyfile, args, Charset.defaultCharset(), configurator, postLaunchAction, stdout, stderr);
    }

    @Test
    public void testLaunch_readFromStdin() throws Exception {
        PipedOutputStream pipeToStdin = new PipedOutputStream();
        PipedInputStream stdinInputPipe = new PipedInputStream(pipeToStdin);
        Charset pipeCharset = StandardCharsets.UTF_8;
        Writer pipeToStdinWriter = new OutputStreamWriter(pipeToStdin, pipeCharset);



        Runnable actionPostLaunch = () -> {
            try {
                pipeToStdinWriter.write(String.format("one%n"));
                Thread.sleep(100);
                pipeToStdinWriter.write(String.format("two%n"));
                Thread.sleep(100);
                pipeToStdinWriter.write(String.format("three%n"));
                pipeToStdinWriter.flush();
                pipeToStdinWriter.close(); // if we don't close this stream, cat waits forever for more data on stdin
            } catch (InterruptedException | IOException e) {
                e.printStackTrace(System.err);
                throw new RuntimeException(e);
            }
        };


        LaunchSupportConfigurator configurator = new LaunchSupportConfigurator() {
            @Override
            public TailingLaunchSupport configure(SubprocessLaunchSupport launchSupport, ExecutorService executorService, StreamTailer tailer) {
                return launchSupport.tailing(executorService, tailer, () -> new CloseShieldInputStream(stdinInputPipe));
            }
        };

        testLaunchAndTail("nht_cat.py", Collections.emptyList(), pipeCharset, configurator, actionPostLaunch, Arrays.asList("one", "two", "three"), Collections.emptyList());

    }

    private interface LaunchSupportConfigurator {
        TailingLaunchSupport configure(SubprocessLaunchSupport launchSupport, ExecutorService executorService, StreamTailer tailer);
    }

    private void testLaunchAndTail(String pyfile,
                                   List<String> args,
                                   Charset tailerCharset,
                                   LaunchSupportConfigurator launchSupportConfigurator,
                                   Runnable actionPostLaunch,
                                   List<String> expectedStdout, List<String> expectedStderr) throws Exception {
        Subprocess subprocess = Tests.runningPythonFile(pyfile)
                .args(args)
                .build();
        ExecutorService relayExecutorService = Executors.newFixedThreadPool(2);
        ProcessResult<Void, Void> result;
        try {
            List<String> stdoutLines = new ArrayList<>(), stderrLines = new ArrayList<>();
            AtomicReference<InterruptedException> interruption = new AtomicReference<>();
            AtomicBoolean streamsAttached = new AtomicBoolean(false);
            StreamTailer tailer = StreamTailer.builder(tailerCharset)
                    .stdoutConsumer(stdoutLines::add)
                    .stderrConsumer(stderrLines::add)
                    .streamAttachmentListener((attached, timeout, ex) -> {
                        streamsAttached.set(attached);
                        interruption.set(ex);
                    })
                    .build();
            SubprocessLaunchSupport ls = subprocess.launcher(TRACKER);
            TailingLaunchSupport tls = launchSupportConfigurator.configure(ls, relayExecutorService, tailer);
            ProcessMonitor<Void, Void> monitor = tls
                    .launch();
            actionPostLaunch.run();
            result = monitor.await(5, TimeUnit.SECONDS);
            System.out.format("stdout: %s%n", stdoutLines);
            System.out.format("stderr: %s%n", stderrLines);
            assertEquals("stdout", expectedStdout, stdoutLines);
            assertEquals("stderr", expectedStderr, stderrLines);
            assertEquals("process result", 0, result.exitCode());
            assertNull("interrupted while waiting for streams", interruption.get());
            assertTrue("expect streams attached", streamsAttached.get());
        } finally {
            relayExecutorService.shutdown();
            boolean terminated = relayExecutorService.awaitTermination(5, TimeUnit.SECONDS);
            assertTrue("terminated", terminated);
        }
    }
}