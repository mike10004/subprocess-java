package io.github.mike10004.subprocess.test;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertFalse;

public class TestsTest {

    @Test
    public void filtering() {
        Properties p = Tests.getProperties();
        assertFalse(p.isEmpty());
        for (String key : p.stringPropertyNames()) {
            assertFalse("not filtered: " + key, Tests.getProperties().getProperty(key).startsWith("${"));
        }
    }
}
