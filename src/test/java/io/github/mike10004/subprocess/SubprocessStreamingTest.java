package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.test.Tests;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.github.mike10004.subprocess.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test of output monitoring and collection facilities.
 */
public class SubprocessStreamingTest extends SubprocessTestBase {

    @ClassRule
    public static TemporaryFolder tmpdir = new TemporaryFolder();

    public SubprocessStreamingTest(int trial) {
        super(trial);
    }

    @Test
    public void testTempFileOutput() throws Exception {
        Subprocess subprocess = Subprocess.running(Tests.getPythonFile("nht_stereo.py"))
                .args("one", "two", "three", "four", "five")
                .build();
        ProcessResult<File, File> result = subprocess.launcher(TRACKER)
                .outputTempFiles(tmpdir.getRoot().toPath())
                .launch().await(5, TimeUnit.SECONDS);
        assertEquals("exit", 0, result.exitCode());
        String stdout = new String(java.nio.file.Files.readAllBytes(result.content().stdout().toPath()), Charset.defaultCharset());
        String stderr = new String(java.nio.file.Files.readAllBytes(result.content().stderr().toPath()), Charset.defaultCharset());
        assertEquals("stdout", String.format("one%nthree%nfive%n"), stdout);
        assertEquals("stderr", String.format("two%nfour%n"), stderr);
    }

    @Test
    public void testLineConsumer() throws Exception {
        Subprocess subprocess = Subprocess.running(Tests.getPythonFile("nht_stereo.py"))
                .args("one", "two", "three", "four", "five")
                .build();
        ExecutorService relayExecutorService = Executors.newFixedThreadPool(2);
        LineConsumerContext context = new LineConsumerContext(relayExecutorService);
        ProcessMonitor<Void, Void> monitor = subprocess.launcher(TRACKER)
                .output(context)
                .launch();
        checkState(monitor.awaitStreamsAttached(5, TimeUnit.SECONDS));
        List<String> stdoutLines = new ArrayList<>(), stderrLines = new ArrayList<>();
        context.startPumping(stdoutLines::add, stderrLines::add);
        ProcessResult<Void, Void> result = monitor.await(5, TimeUnit.SECONDS);
        relayExecutorService.shutdown();
        boolean terminated = relayExecutorService.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue("terminated", terminated);
        assertEquals("stdout", Arrays.asList("one", "three", "five"), stdoutLines);
        assertEquals("stderr", Arrays.asList("two", "four"), stderrLines);
        assertEquals("process result", 0, result.exitCode());
    }

    private static class AwaitableReference<T> {

        private final AtomicReference<T> holder;
        private final CountDownLatch latch;

        public AwaitableReference() {
            holder = new AtomicReference<>();
            latch = new CountDownLatch(1);
        }

        public void assign(T value) {
            requireNonNull(value);
            T previous = holder.getAndSet(value);
            if (previous == null) {
                latch.countDown();
            } else {
                // in other contexts, we might want to return without throwing an exception here
                throw new IllegalStateException("value has already been assigned");
            }
        }

        @Nullable
        public T await(long timeout, TimeUnit unit) throws InterruptedException {
            boolean gotIt = latch.await(timeout, unit);
            if (gotIt) {
                return holder.get();
            } else {
                return null;
            }
        }
    }

    private static class LineConsumerControl implements StreamControl {

        private final AwaitableReference<InputStream> stdout, stderr;

        public LineConsumerControl() {
            this.stdout = new AwaitableReference<>();
            this.stderr = new AwaitableReference<>();
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return openSink(stdout);
        }

        private OutputStream openSink(AwaitableReference<InputStream> ref) throws IOException {
            StreamPipeSink sink = new StreamPipeSink();
            StreamPipe.ComponentPair<InputStream, OutputStream> components = sink.createComponents();
            ref.assign(components.from);
            return components.to;
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return openSink(stderr);
        }

        @Nullable
        @Override
        public InputStream openStdinSource() {
            return null;
        }
    }

    private static class LineConsumerContext implements StreamContext<LineConsumerControl, Void, Void> {

        private final ExecutorService relayExecutorService;
        private transient volatile LineConsumerControl control;

        public LineConsumerContext(ExecutorService relayExecutorService) {
            this.relayExecutorService = relayExecutorService;
        }

        @Override
        public LineConsumerControl produceControl() {
            return control = new LineConsumerControl();
        }

        public void startPumping(Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer) throws InterruptedException {
            checkState(control != null, "control not yet created");
            InputStream stdoutSource = control.stdout.await(1, TimeUnit.SECONDS);
            checkState(stdoutSource != null, "stdout not ready");
            relayExecutorService.submit(new ReaderRunnable(stdoutConsumer, stdoutSource, Charset.defaultCharset()));
            InputStream stderrSource = control.stderr.await(1, TimeUnit.SECONDS);
            checkState(stderrSource != null, "stderr not ready");
            relayExecutorService.submit(new ReaderRunnable(stderrConsumer, stderrSource, Charset.defaultCharset()));
        }

        private static class ReaderRunnable implements Callable<Void> {

            private final Consumer<String> consumer;
            private final InputStream in;
            private final Charset charset;

            public ReaderRunnable(Consumer<String> consumer, InputStream in, Charset charset) {
                this.consumer = consumer;
                this.in = in;
                this.charset = charset;
            }

            @Override
            public Void call() throws IOException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
                String line;
                while ((line = reader.readLine()) != null) {
                    consumer.accept(line);
                }
                return (Void) null;
            }
        }

        @Override
        public StreamContent<Void, Void> transform(int exitCode, LineConsumerControl context) {
            return StreamContent.absent();
        }
    }
}
