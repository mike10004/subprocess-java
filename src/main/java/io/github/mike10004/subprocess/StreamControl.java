package io.github.mike10004.subprocess;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface that defines methods used to interact with the input and output streams
 * of a process.
 */
public interface StreamControl {

    /**
     * Opens the stream where process standard output data is to be written.
     * @return an open output stream
     * @throws IOException on I/O error
     */
    OutputStream openStdoutSink() throws IOException;

    /**
     * Opens the stream where process standard error data is to be written.
     * @return an open output stream
     * @throws IOException on I/O error
     */
    OutputStream openStderrSink() throws IOException;

    /**
     * Opens the stream from which process standard input is to be read.
     * @return an open input stream
     * @throws IOException on I/O error
     */
    @Nullable
    InputStream openStdinSource() throws IOException;

}
