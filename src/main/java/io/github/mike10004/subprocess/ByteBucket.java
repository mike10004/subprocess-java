package io.github.mike10004.subprocess;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

class ByteBucket implements StreamOutput {

    private final ByteArrayOutputStream collector;

    public ByteBucket(ByteArrayOutputStream collector) {
        this.collector = collector;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public OutputStream openStream() throws IOException {
        return collector;
    }

    public String decode(Charset charset) {
        return new String(dump(), charset);
    }

    public byte[] dump() {
        return collector.toByteArray();
    }

    public static ByteBucket withInitialCapacity(int capacity) {
        return new ByteBucket(new ByteArrayOutputStream(capacity));
    }

    public static ByteBucket create() {
        return new ByteBucket(new ByteArrayOutputStream());
    }

    public String toString() {
        return "ByteBucket[" + collector.size() + "]";
    }
}
