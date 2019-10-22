package io.github.mike10004.subprocess;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.github.mike10004.subprocess.Preconditions.checkState;

/**
 * Stream context that relays lines of process output to consumers.
 * Instances of this class must not be reused as arguments to
 * {@link SubprocessLaunchSupport#output(StreamContext)}.
 */
public class LineConsumerContext implements NonCapturingStreamContext<StreamControl> {

    private transient volatile LineConsumerControl control;

    /**
     * Constructs an instance of the class class.
     * The {@link #produceControl()} method may be invoked at most once on an instance of this class.
     */
    public LineConsumerContext() {
    }

    @Override
    public StreamControl produceControl() {
        checkState(control == null, "control has already been produced by this stream context instance; do not reuse these instances");
        return control = new LineConsumerControl();
    }

    /**
     * Start relaying process output lines, decoding process output using the platform default charset.
     *
     * @param relayExecutorService executor service to which standard output and standard error relay loops
     * are to be submitted
     * @param stdoutConsumer consumer of lines printed on process standard output
     * @param stderrConsumer consumer of lines printed on process standard error
     * @see #startRelaying(ExecutorService, Charset, Consumer, Consumer)
     */
    public void startRelaying(ExecutorService relayExecutorService, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer) {
        Charset outputCharset = Charset.defaultCharset();
        startRelaying(relayExecutorService, outputCharset, stdoutConsumer, stderrConsumer);
    }

    /**
     * Start relaying process output lines, decoding process output using the given charset.
     * Be sure that the streams are available before invoking this method by executing
     * {@link ProcessMonitor#awaitStreamsAttached(long, TimeUnit)}.
     * In this implementation, a runnable that executes a loop in which content is relayed from a process
     * stream to a line consumer is created for each of the process standard output and
     * standard error streams. Each of those relay loops is submitted to the given executor
     * service. A reasonable value for the executor service would be an instance created by
     * {@link java.util.concurrent.Executors#newFixedThreadPool(int) Executors.newFixedThreadPool(2)},
     * where the number of threads is 2 so that process standard output and standard error
     * can be consumed concurrently.
     * @param relayExecutorService executor service to which standard output and standard error relay loops
     * are to be submitted
     * @param stdoutConsumer consumer of lines printed on process standard output
     * @param stderrConsumer consumer of lines printed on process standard error
     * @see #startRelaying(ExecutorService, Charset, Consumer, Consumer)
     */
    public void startRelaying(ExecutorService relayExecutorService, Charset outputCharset, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer) {
        checkState(control != null, "control not yet created; obtain true result from ProcessMonitor.awaitStreamsAttached() first");
        InputStream stdoutSource = control.getStdoutStream();
        relayExecutorService.submit(new LineRelayer(stdoutConsumer, stdoutSource, outputCharset));
        InputStream stderrSource = control.getStderrStream();
        relayExecutorService.submit(new LineRelayer(stderrConsumer, stderrSource, outputCharset));
    }

    private static class LineRelayer implements Callable<Void> {

        private final Consumer<String> consumer;
        private final InputStream in;
        private final Charset charset;

        public LineRelayer(Consumer<String> consumer, InputStream in, Charset charset) {
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

    private class LineConsumerControl implements StreamControl {

        private final AtomicReference<InputStream> stdoutIn, stderrIn;
        private final AtomicReference<OutputStream> stdoutOut, stderrOut;

        public LineConsumerControl() {
            this.stdoutIn = new AtomicReference<>();
            this.stderrIn = new AtomicReference<>();
            this.stdoutOut = new AtomicReference<>();
            this.stderrOut = new AtomicReference<>();
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return openSink(stdoutIn, stdoutOut);
        }

        private OutputStream openSink(AtomicReference<InputStream> inRef, AtomicReference<OutputStream> outRef) throws IOException {
            PipedInputStream pipe;
            inRef.set(pipe = new PipedInputStream());
            OutputStream retval;
            outRef.set(retval = new PipedOutputStream(pipe));
            return retval;
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return openSink(stderrIn, stderrOut);
        }

        @Nullable
        @Override
        public InputStream openStdinSource() {
            return LineConsumerContext.this.openStdinSourceForControl();
        }

        public InputStream getStdoutStream() {
            return getValueOrDie(stdoutIn, "stdout");
        }

        public InputStream getStderrStream() {
            return getValueOrDie(stderrIn, "stderr");
        }

        private <T> T getValueOrDie(AtomicReference<T> ref, String label) {
            T value = ref.get();
            checkState(value != null, "%s not yet assigned; obtain true result from ProcessMonitor.awaitStreamsAttached() first", label);
            return value;
        }
    }

    /**
     * Returns the input stream instance used to pass data to the process's standard input.
     * By default, this method returns null, which means no data is to be fed to the process.
     * This method may be overridden to provide a data to the process.
     * @return a new input stream, or null
     */
    @Nullable
    protected InputStream openStdinSourceForControl() {
        return null;
    }
}
