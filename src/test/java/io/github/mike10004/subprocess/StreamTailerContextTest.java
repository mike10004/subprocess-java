package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.test.Tests;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.mike10004.subprocess.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StreamTailerContextTest extends SubprocessTestBase {

    public StreamTailerContextTest(int trial) {
        super(trial);
    }

    @Test
    public void testLineConsumer() throws Exception {
        Subprocess subprocess = Tests.runningPythonFile("nht_stereo.py")
                .args("one", "two", "three", "four", "five")
                .build();
        ExecutorService relayExecutorService = Executors.newFixedThreadPool(2);
        StreamTailerContext context = new StreamTailerContext();
        ProcessMonitor<Void, Void> monitor = subprocess.launcher(TRACKER)
                .output(context)
                .launch();
        checkState(monitor.awaitStreamsAttached(5, TimeUnit.SECONDS));
        List<String> stdoutLines = new ArrayList<>(), stderrLines = new ArrayList<>();
        context.startRelaying(relayExecutorService, Charset.defaultCharset(), stdoutLines::add, stderrLines::add);
        ProcessResult<Void, Void> result = monitor.await(5, TimeUnit.SECONDS);
        relayExecutorService.shutdown();
        boolean terminated = relayExecutorService.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue("terminated", terminated);
        System.out.format("stdout: %s%n", stdoutLines);
        System.out.format("stderr: %s%n", stderrLines);
        assertEquals("stdout", Arrays.asList("one", "three", "five"), stdoutLines);
        assertEquals("stderr", Arrays.asList("two", "four"), stderrLines);
        assertEquals("process result", 0, result.exitCode());
    }

}
