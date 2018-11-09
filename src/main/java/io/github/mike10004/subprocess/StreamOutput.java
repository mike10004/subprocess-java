package io.github.mike10004.subprocess;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface of a sink to which a byte stream is to be written.
 */
public interface StreamOutput {

    /**
     * Opens an output stream.
     * @return an open output stream
     * @throws IOException on I/O error
     */
    OutputStream openStream() throws IOException;

    /**
     * Returns a sink to which bytes are written and ignored.
     * @return a sink
     */
    static StreamOutput abyss() {
        return Streams::nullOutputStream;
    }
}
