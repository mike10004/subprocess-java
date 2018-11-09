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

    OutputStream openStdoutSink() throws IOException;
    OutputStream openStderrSink() throws IOException;
    @Nullable
    InputStream openStdinSource() throws IOException;

}
