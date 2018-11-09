package io.github.mike10004.subprocess;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import io.github.mike10004.subprocess.test.Tests;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class SubprocessPipingTest extends SubprocessTestBase {

    @Test(timeout = 5000L)
    public void launch_readInput_piped() throws Exception {
        Charset charset = UTF_8;
        StreamPipeSource pipe = new StreamPipeSource();
        ProcessMonitor<String, String> monitor = Tests.runningPythonFile(Tests.pyReadInput())
                .build()
                .launcher(TRACKER)
                .outputStrings(charset, pipe.asByteSource())
                .launch();
        List<String> lines = Arrays.asList("foo", "bar", "baz", "");
        PrintWriter printer = new PrintWriter(new OutputStreamWriter(pipe.connect(), charset));
        for (String line : lines) {
            printer.println(line);
            printer.flush();
        }
        ProcessResult<String, String> result = monitor.await();
        System.out.format("get() returned %s%n", result);
        assertEquals("exit code", 0, result.exitCode());
        String expected = Tests.joinPlus(System.lineSeparator(), lines.subList(0, 3));
        assertEquals("output", expected, result.content().stdout());
    }

    private static final List<String> poemLines = Arrays.asList(
            "April is the cruellest month, breeding",
            "Lilacs out of the dead land, mixing",
            "Memory and desire, stirring",
            "Dull roots with spring rain.",
            "Winter kept us warm, covering",
            "Earth in forgetful snow, feeding",
            "A little life with dried tubers.");

    private static File writePoemToFile() throws IOException {
        File wastelandFile = File.createTempFile("SubprocessTest", ".txt");
        Files.asCharSink(wastelandFile, UTF_8).writeLines(poemLines);
        return wastelandFile;
    }

    @SuppressWarnings("Duplicates")
    @Test(timeout = 5000L)
    public void listen_pipeClass() throws Exception {
        org.apache.commons.io.input.TeeInputStream.class.getName();
        File wastelandFile = writePoemToFile();
        ByteBucket stderrBucket = ByteBucket.create();
        StreamPipeSink stdoutPipe = new StreamPipeSink();
        PredefinedStreamControl endpoints = PredefinedStreamControl.builder()
                .stderr(stderrBucket)
                .stdout(stdoutPipe.asByteSink())
                .noStdin() // read from file passed as argument
                .build();
        StreamContext<?, Void, String> outputControl = StreamContext.predefined(endpoints, nullSupplier(), () -> stderrBucket.decode(Charset.defaultCharset()));
        ProcessMonitor<Void, String> monitor = Tests.runningPythonFile(Tests.pyCat())
                .arg(wastelandFile.getAbsolutePath())
                .build()
                .launcher(TRACKER)
                .output(outputControl)
                .launch();
        Charset charset = Charset.defaultCharset();
        List<String> actualLines;
        // read from the process output stream while the process executes
        try (Reader reader = new InputStreamReader(stdoutPipe.connect(), charset)) {
            /*
             * It's possible to get an IOException here when the process has not yet finished
             * (so PipedOutputStream.receivedLast has not been invoked) but the thread on which
             * data was being written is dead.
             */
            actualLines = CharStreams.readLines(reader);
        }
        ProcessResult<Void, String> result = monitor.await();
        System.out.format("result: %s%n", result);
        System.out.format("lines:%n%s%n", String.join(System.lineSeparator(), actualLines));
        assertEquals("actual", poemLines, actualLines);
        assertEquals("exit code", 0, result.exitCode());
    }

    @SuppressWarnings("Duplicates")
    @Test(timeout = 5000L)
    public void listen_pipe_interleaved() throws Exception {
        ByteBucket stderrBucket = ByteBucket.create();
        StreamPipeSink stdoutPipe = new StreamPipeSink();
        StreamPipeSource stdinPipe = new StreamPipeSource();
        PredefinedStreamControl endpoints = PredefinedStreamControl.builder()
                .stderr(stderrBucket)
                .stdout(stdoutPipe.asByteSink())
                .stdin(stdinPipe.asByteSource())
                .build();
        StreamContext<?, Void, String> outputControl = StreamContext.predefined(endpoints, nullSupplier(), () -> stderrBucket.decode(Charset.defaultCharset()));
        ProcessMonitor<Void, String> monitor =Tests.runningPythonFile(Tests.pyReadInput())
                .build()
                .launcher(TRACKER)
                .output(outputControl)
                .launch();
        Charset charset = Charset.defaultCharset();
        System.out.format("expecting poem: %n%s%n", String.join(System.lineSeparator(), poemLines));
        List<String> actualLines = new ArrayList<>(poemLines.size());
        try (PrintWriter pipeWriter = new PrintWriter(new OutputStreamWriter(stdinPipe.connect(), charset));
             BufferedReader pipeReader = new BufferedReader(new InputStreamReader(stdoutPipe.connect(), charset))) {
            for (String line : poemLines) {
                pipeWriter.println(line);
                pipeWriter.flush();
                actualLines.add(pipeReader.readLine());
            }
        }
        ProcessResult<Void, String> result = monitor.await();
        System.out.format("result: %s%n", result);
        if (result.exitCode() != 0) {
            System.err.println(result.content().stderr());
        }
        System.out.format("lines:%n%s%n", String.join(System.lineSeparator(), actualLines));
        assertEquals("actual", poemLines, actualLines);
        assertEquals("exit code", 0, result.exitCode());
    }
}
