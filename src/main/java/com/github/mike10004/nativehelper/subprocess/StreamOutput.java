package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.io.OutputStream;

public interface StreamOutput {

    OutputStream openStream() throws IOException;

    static StreamOutput abyss() {
        return Streams::nullOutputStream;
    }
}
