package com.github.mike10004.nativehelper.subprocess;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringEscapeUtilsTest {

    @Test
    public void escapeJava() {
        String unescaped = "/ˈfɑnɪks/\n";
        System.out.println(StringEscapeUtils.escapeJava(unescaped));
    }
}