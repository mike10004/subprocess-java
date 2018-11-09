package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.ByteSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

class ByteBucket implements ByteSink {

    private final ByteArrayOutputStream collector;

    public ByteBucket(ByteArrayOutputStream collector) {
        this.collector = collector;
    }

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
