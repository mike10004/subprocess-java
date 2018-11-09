package io.github.mike10004.subprocess;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProcessResultTest {

    @Test
    public void map() {
        ProcessResult<Integer, Integer> a = ProcessResult.direct(0, 1, 2);
        ProcessResult<String, Void> b = a.map(String::valueOf, x -> (Void) null);
        assertEquals("1", b.content().stdout());
        assertNull(b.content().stderr());
    }
}