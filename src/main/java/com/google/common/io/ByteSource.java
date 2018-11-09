package com.google.common.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

public interface ByteSource {

    InputStream openStream() throws IOException;

    static ByteSource empty() {
        return () -> new ByteArrayInputStream(new byte[0]);
    }

    static ByteSource wrap(byte[] bytes) {
        requireNonNull(bytes);
        return () -> new ByteArrayInputStream(bytes);
    }
}
