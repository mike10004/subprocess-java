package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class StreamPipeSource extends StreamPipe<OutputStream, InputStream> {

    @Override
    protected ComponentPair<OutputStream, InputStream> createComponents() throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        return ComponentPair.of(out, in);
    }

    public StreamInput asByteSource() {
        return this::openTo;
    }
}
