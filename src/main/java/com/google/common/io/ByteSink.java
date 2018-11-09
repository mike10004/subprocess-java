package com.google.common.io;

import java.io.IOException;
import java.io.OutputStream;

public interface ByteSink {

    OutputStream openStream() throws IOException;

    static ByteSink abyss() {
        return ByteStreams::nullOutputStream;
    }
}
