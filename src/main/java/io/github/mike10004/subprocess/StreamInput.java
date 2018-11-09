package io.github.mike10004.subprocess;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

public interface StreamInput {

    InputStream openStream() throws IOException;

    static StreamInput empty() {
        return () -> new ByteArrayInputStream(new byte[0]);
    }

    static StreamInput wrap(byte[] bytes) {
        requireNonNull(bytes);
        return () -> new ByteArrayInputStream(bytes);
    }

    default byte[] read() throws IOException {
        try (InputStream in = openStream()) {
            return Streams.toByteArray(in);
        }
    }
}
