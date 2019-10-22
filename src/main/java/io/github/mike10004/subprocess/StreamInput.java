package io.github.mike10004.subprocess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
        return new Streams.EmptyStreamInput();
    }

    static StreamInput wrap(byte[] bytes) {
        return new Streams.MemoryStreamInput(bytes);
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
