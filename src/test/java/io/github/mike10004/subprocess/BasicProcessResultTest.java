package io.github.mike10004.subprocess;

import org.junit.Test;

import static org.junit.Assert.*;

public class BasicProcessResultTest {

    @Test
    public void withNoOutput() {
        BasicProcessResult<?, ?> result = BasicProcessResult.withNoOutput(3);
        assertEquals(3, result.exitCode());
        assertNull(result.content().stdout());
        assertNull(result.content().stderr());
    }
}