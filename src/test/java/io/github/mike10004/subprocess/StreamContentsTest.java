package io.github.mike10004.subprocess;

import org.junit.Test;

import static org.junit.Assert.*;

public class StreamContentsTest {

    @Test
    public void testToString_byteArrays() {
        StreamContent<?, ?> p = StreamContent.direct(new byte[3], new byte[7]);
        assertEquals("toString()", "DirectOutput{stdout=byte[3], stderr=byte[7]}", p.toString());
    }

    @Test
    public void testToString_strings() {
        StreamContent<?, ?> p = StreamContent.direct("blah", null);
        assertEquals("toString()", "DirectOutput{stdout=\"blah\", stderr=}", p.toString());
    }
}