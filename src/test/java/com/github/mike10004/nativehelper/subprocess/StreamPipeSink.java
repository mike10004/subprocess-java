package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class StreamPipeSink extends StreamPipe<InputStream, OutputStream> {

    @Override
    protected ComponentPair<InputStream, OutputStream> createComponents() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        return ComponentPair.of(in, out);
    }

    public StreamOutput asByteSink() {
        return this::openTo;
    }
}
