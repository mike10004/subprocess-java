package com.github.mike10004.nativehelper.test;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class TestsTest {

    @Test
    public void filtering() throws Exception {
        for (String key : Tests.getProperties().stringPropertyNames()) {
            assertFalse("not filtered: " + key, Tests.getProperties().getProperty(key).startsWith("${"));
        }
    }
}
