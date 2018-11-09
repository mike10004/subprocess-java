package io.github.mike10004.subprocess.test;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ProcessTracker;
import io.github.mike10004.subprocess.StreamContent;
import io.github.mike10004.subprocess.StreamContext;
import io.github.mike10004.subprocess.StreamControl;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * Test that confirms we can implement common use cases without the package-private API.
 */
public class ExternalPackageTest {

    @Rule
    public ProcessTrackerRule processContextRule = new ProcessTrackerRule();

    private ProcessTracker CONTEXT;

    @Before
    public void setUp() {
        CONTEXT = processContextRule.getTracker();
    }

    @Test
    public void captureOutputAsStrings() throws Exception {
        ProcessResult<String, String> result = fixture.subprocess.launcher(CONTEXT).outputStrings(Charset.defaultCharset()).launch().await();
        System.out.format("result: %s%n", result);
        fixture.check(result, Function.identity());
    }

    @Test
    public void listenToPipedOutput() throws Exception {
        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout);
        StreamControl ctx = new StreamControl() {
            @Override
            public OutputStream openStdoutSink() {
                return pout;
            }

            @Override
            public OutputStream openStderrSink() {
                return ByteStreams.nullOutputStream();
            }

            @Nullable
            @Override
            public InputStream openStdinSource() {
                return null;
            }
        };
        StreamContext<?, Void, Void> outputControl = new StreamContext<StreamControl, Void, Void>() {
            @Override
            public StreamControl produceControl() {
                return ctx;
            }

            @Override
            public StreamContent<Void, Void> transform(int exitCode, StreamControl context) {
                return StreamContent.absent();
            }
        };
        ProcessMonitor<Void, Void> monitor = fixture.subprocess.launcher(CONTEXT).output(outputControl).launch();
        byte[] stdoutcontent = ByteStreams.toByteArray(pin);
        ProcessResult<Void, Void> result = monitor.await();
        assertEquals("exit code", 0, result.exitCode());
        String actualStdout = new String(stdoutcontent, StandardCharsets.US_ASCII);
        assertEquals("stdout", fixture.expectedStdout, actualStdout);
    }

    @Test
    public void captureOutputToFile() throws Exception {
        File stderrFile = File.createTempFile("ExternalPackageTest", ".txt");
        File stdoutFile = File.createTempFile("ExternalPackageTest", ".txt");
        ProcessResult<File, File> result = fixture.subprocess.launcher(CONTEXT)
                .outputFiles(stdoutFile, stderrFile)
                .launch()
                .await();
        System.out.format("result: %s%n", result);
        fixture.check(result, readContentsFunction(Charset.defaultCharset()));
    }

    @Test
    public void captureOutputToFilesInTempDir() throws Exception {
        Path tempDir = FileUtils.getTempDirectory().toPath();
        ProcessResult<File, File> result = fixture.subprocess.launcher(CONTEXT)
                .outputTempFiles(tempDir)
                .launch()
                .await();
        System.out.format("result: %s%n", result);
        fixture.check(result, readContentsFunction(Charset.defaultCharset()));
    }


    private static Function<File, String> readContentsFunction(Charset charset) {
        return file -> {
            try {
                return Files.asCharSource(file, charset).read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static final Fixture fixture = new Fixture();

    private static class Fixture {
        public final Subprocess subprocess;
        public final String expectedStdout, expectedStderr;

        public Fixture() {
            String arg1 = "foo", arg2 = "bar";
            subprocess = Tests.runningPythonFile(Tests.getPythonFile("nht_stereo.py"))
                    .args(arg1, arg2)
                    .build();
            expectedStdout = String.format("%s%n", arg1);
            expectedStderr = String.format("%s%n", arg2);
        }

        private void check(int exitCode, String stdout, String stderr) {
            assertEquals("exit code", 0, exitCode);
            assertEquals("stdout", expectedStdout, stdout);
            assertEquals("stderr", expectedStderr, stderr);
        }

        public <S> void check(ProcessResult<S, S> result, Function<? super S, String> outputMap) {
            check(result.exitCode(), outputMap.apply(result.content().stdout()), outputMap.apply(result.content().stderr()));
        }
    }
}
