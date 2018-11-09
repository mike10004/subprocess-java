package io.github.mike10004.subprocess;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a source of a byte stream.
 */
public interface StreamInput {

    /**
     * Opens an input stream.
     * @return an open input stream
     * @throws IOException on I/O error
     */
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

    static StreamInput fromFile(File file) {
        return new Streams.FileStreamInput(file);
    }
}
