package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.test.Tests;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test of output monitoring and collection facilities.
 */
public class SubprocessStreamingTest extends SubprocessTestBase {

    @ClassRule
    public static TemporaryFolder tmpdir = new TemporaryFolder();

    public SubprocessStreamingTest(int trial) {
        super(trial);
    }

    @Test
    public void testTempFileOutput() throws Exception {
        Subprocess subprocess = Tests.runningPythonFile("nht_stereo.py")
                .args("one", "two", "three", "four", "five")
                .build();
        ProcessResult<File, File> result = subprocess.launcher(TRACKER)
                .outputTempFiles(tmpdir.getRoot().toPath())
                .launch().await(5, TimeUnit.SECONDS);
        assertEquals("exit", 0, result.exitCode());
        String stdout = new String(java.nio.file.Files.readAllBytes(result.content().stdout().toPath()), Charset.defaultCharset());
        String stderr = new String(java.nio.file.Files.readAllBytes(result.content().stderr().toPath()), Charset.defaultCharset());
        assertEquals("stdout", String.format("one%nthree%nfive%n"), stdout);
        assertEquals("stderr", String.format("two%nfour%n"), stderr);
    }

}
