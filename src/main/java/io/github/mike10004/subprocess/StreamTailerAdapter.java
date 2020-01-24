package io.github.mike10004.subprocess;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.StringJoiner;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class StreamTailerAdapter implements StreamTailer {

    private final Consumer<? super String> stdoutConsumer;
    private final Consumer<? super String> stderrConsumer;
    private final Charset charset;
    private final Duration streamAttachTimeout;
    private final StreamAttachmentListener streamAttachmentListener;

    public StreamTailerAdapter(Consumer<? super String> stdoutConsumer, Consumer<? super String> stderrConsumer, Charset charset, Duration streamAttachTimeout, StreamAttachmentListener streamAttachmentListener) {
        this.stdoutConsumer = requireNonNull(stdoutConsumer);
        this.stderrConsumer = requireNonNull(stderrConsumer);
        this.charset = requireNonNull(charset);
        this.streamAttachTimeout = requireNonNull(streamAttachTimeout);
        this.streamAttachmentListener = requireNonNull(streamAttachmentListener);
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public Duration streamAttachTimeout() {
        return streamAttachTimeout;
    }

    @Override
    public void acceptStdout(String line) {
        stdoutConsumer.accept(line);
    }

    @Override
    public void acceptStderr(String line) {
        stderrConsumer.accept(line);
    }

    @Override
    public StreamAttachmentListener streamAttachmentListener() {
        return streamAttachmentListener;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StreamTailerAdapter.class.getSimpleName() + "[", "]")
                .add("stdoutConsumer=" + stdoutConsumer)
                .add("stderrConsumer=" + stderrConsumer)
                .add("charset=" + charset)
                .add("streamAttachTimeout=" + streamAttachTimeout)
                .add("streamAttachmentListener=" + streamAttachmentListener)
                .toString();
    }

}
