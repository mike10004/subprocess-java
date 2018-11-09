package io.github.mike10004.subprocess;

import org.junit.Test;

import static org.junit.Assert.*;

public class StreamContentTest {

    @Test
    public void absent() {
        StreamContent<String, String> empty = StreamContent.absent();
        assertNull(empty.stdout());
        assertNull(empty.stderr());
    }
}