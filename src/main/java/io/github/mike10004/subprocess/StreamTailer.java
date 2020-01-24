package io.github.mike10004.subprocess;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a service that tails process output. To tail output is to consume
 * it line by line.
 */
public interface StreamTailer {

    /**
     * Returns the charset in which characters of each process output stream are encoded.
     * For platform-native executables, this is commonly the platform default charset.
     * @return a charset
     * @see Charset#defaultCharset()
     */
    Charset charset();

    /**
     * Returns the amount of time to wait for streams that read process output to
     * be attached to the process. There is a delay between when the process is
     * launched and output streams from the process are opened and readable.
     * It is often extremely short, but the default in this implementation is 10 seconds.
     * When the streams have attached or the timeout has elapsed, the
     * {@link #streamAttachTimeoutElapsed(boolean, Duration)} method is invoked, and its
     * argument specifies whether the reader streams did in fact attach to the process.
     * The waiting may also be interrupted, in which
     * @return time to wait for stream attachment
     */
    Duration streamAttachTimeout();

    /**
     * Method invoked when a line of process standard output stream is read.
     * @param line the line
     */
    void acceptStdout(String line);

    /**
     * Method invoked when a line of process standard error stream is read.
     * @param line the line
     */
    void acceptStderr(String line);

    /**
     * Returns a stream attachment listener. The listener is notified of whether
     * stream attachment has succeeded or failed. If stream attachment failed,
     * no lines will ever be relayed to the {@link #acceptStderr(String)}
     * or {@link #acceptStdout(String)} methods, even if the process continues
     * executing. It might be worthwhile to kill such a process, if its only
     * purpose was to provide output.
     * @return a stream attachment listener
     */
    StreamAttachmentListener streamAttachmentListener();

    static Builder builder(Charset charset) {
        return new Builder(charset);
    }

    /**
     * Interface of a service that is notified when stream attachment has succeeded or failed.
     */
    interface StreamAttachmentListener {
        /**
         * Method invoked when the wait for stream attachment is over, whether the attachment succeeded or failed.
         * @param attached flag indicating whether streams attached
         * @param durationWaited duration waited for streams to attach
         * @param interruption interruption exception thrown while waiting, or null if no such interruption occurred
         */
        void streamAttachmentWaitFinished(boolean attached, Duration durationWaited, @Nullable InterruptedException interruption);
    }

    final class Builder {
        private Consumer<? super String> stdoutConsumer = ignore -> {};
        private Consumer<? super String> stderrConsumer = ignore -> {};
        private final Charset charset;
        private Duration streamAttachTimeout = Duration.ofSeconds(10);
        private StreamAttachmentListener streamAttachmentListener = (ignore1, ignore2, ignore3) -> {};

        private Builder(Charset charset) {
            this.charset = requireNonNull(charset);
        }

        public Builder stdoutConsumer(Consumer<? super String> val) {
            stdoutConsumer = requireNonNull(val);
            return this;
        }

        public Builder stderrConsumer(Consumer<? super String> val) {
            stderrConsumer = requireNonNull(val);
            return this;
        }

        public Builder streamAttachTimeout(Duration val) {
            streamAttachTimeout = requireNonNull(val);
            return this;
        }

        public Builder streamAttachmentListener(StreamAttachmentListener streamAttachmentListener) {
            this.streamAttachmentListener = requireNonNull(streamAttachmentListener);
            return this;
        }

        public StreamTailerAdapter build() {
            return new StreamTailerAdapter(stdoutConsumer, stderrConsumer, charset, streamAttachTimeout, streamAttachmentListener);
        }
    }
}

