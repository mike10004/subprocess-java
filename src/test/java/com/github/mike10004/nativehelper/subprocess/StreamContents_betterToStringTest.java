package com.github.mike10004.nativehelper.subprocess;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.awt.Color;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class StreamContents_betterToStringTest {

    private TestCase testCase;

    public StreamContents_betterToStringTest(TestCase testCase) {
        this.testCase = testCase;
    }

    @Parameters
    public static List<TestCase> testCases() {
        return ImmutableList.<TestCase>builder()
                .add(new TestCase(123, "123"))
                .add(new TestCase(new String[10], "String[10]"))
                .add(new TestCase(new Integer[0], "Integer[0]"))
                .add(new TestCase(new String[0], "String[0]"))
                .add(new TestCase(new Integer[0][2], "[Ljava.lang.Integer;[0]", "this one is suboptimal"))
                .add(new TestCase(new String[3][4][5], "String[3][4][5]"))
                .add(new TestCase(new int[3][4][5], "int[3][4][5]", "primitive"))
                .add(new TestCase(new Double[5], "Double[5]"))
                .add(new TestCase(new java.lang.reflect.Type[][]{
                        new java.lang.reflect.Type[]{StreamContents_betterToStringTest.class}
                }, "java.lang.reflect.Type[1][1]", "java.lang.reflect"))
                .add(new TestCase(new Color[][]{
                        new Color[]{Color.RED, Color.GREEN, Color.BLUE}
                }, "java.awt.Color[1][3]"))
                .add(new TestCase(mixedTypes(), "String[2][3]"))
                .build();
    }

    private static Object mixedTypes() {
        java.lang.reflect.Type t;
        Object[] top = {
                new String[3],
                new String[3]
        };
        return top;
    }

    @Test
    public void betterToString() {
        String actual = StreamContents.betterToString(testCase.object);
        String desc = testCase.description == null ? "result for " + testCase.object : testCase.description;
        assertEquals(desc, testCase.expected, actual);
    }

    private static class TestCase {
        public final Object object;
        public final String expected;
        public final String description;

        private TestCase(Object object, String expected) {
            this(object, expected, null);
        }

        private TestCase(Object object, String expected, String description) {
            this.object = object;
            this.expected = expected;
            this.description = description;
        }
    }

}