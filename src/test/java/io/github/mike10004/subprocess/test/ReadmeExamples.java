package io.github.mike10004.subprocess.test;

import io.github.mike10004.subprocess.BasicSubprocessLauncher;
import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.SigtermAttempt;
import io.github.mike10004.subprocess.StreamContent;
import io.github.mike10004.subprocess.StreamContext;
import io.github.mike10004.subprocess.StreamControl;
import io.github.mike10004.subprocess.StreamInput;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.SubprocessLauncher;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class ReadmeExamples {

    public static void main(String[] args) throws Exception {
        Example_LaunchProcessAndIgnoreOutput.main(args);
        Example_CaptureProcessOutputAsStrings.main(args);
        Example_CaptureProcessOutputInFiles.main(args);
        Example_FeedStandardInputToProcess.main(args);
        Example_TerminateProcess.main(args);
        Example_TailProcessOutput.main(args);
    }

    public static class Example_LaunchProcessAndIgnoreOutput {

        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_nonFluentInterface");
            // README_SNIPPET readme_example_nonFluentInterface
            Subprocess subprocess = Subprocess.running("true").build();
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                SubprocessLauncher launcher = new BasicSubprocessLauncher(processTracker);
                ProcessMonitor<?, ?> monitor = launcher.launch(subprocess);
                ProcessResult<?, ?> result = monitor.await();
                System.out.println("exit with status " + result.exitCode());
            }
            // README_SNIPPET readme_example_nonFluentInterface
        }
    }

    public static class Example_FluentlyLaunchProcessAndIgnoreOutput {

        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_fluentInterface");
            // README_SNIPPET readme_example_fluentInterface
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                int exitCode = Subprocess.running("true").build()
                        .launcher(processTracker)
                        .launch()
                        .await()
                        .exitCode();
                System.out.println("exit with status " + exitCode);
            }
            // README_SNIPPET readme_example_fluentInterface
        }
    }

    public static class Example_CaptureProcessOutputAsStrings {

        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_launchAndCaptureStrings");
            // README_SNIPPET readme_example_launchAndCaptureStrings
            // <String, String> parameters refer to type of captured stdout and stderr data
            ProcessResult<String, String> result;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                result = Subprocess.running("echo")
                        .arg("hello, world")
                        .build()
                        .launcher(processTracker)
                        .outputStrings(Charset.defaultCharset())
                        .launch()
                        .await();
            }
            System.out.println(result.content().stdout()); // prints "hello, world"
            // README_SNIPPET readme_example_launchAndCaptureStrings
        }

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static class Example_CaptureProcessOutputInFiles {
        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_launchAndCaptureFiles");
            // README_SNIPPET readme_example_launchAndCaptureFiles
            ProcessResult<File, File> result;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                ProcessMonitor<File, File> monitor = Subprocess.running("echo")
                        .arg("0123456789")
                        .build()
                        .launcher(processTracker)
                        .outputTempFiles(new File(System.getProperty("java.io.tmpdir")).toPath())
                        .launch();
                result = monitor.await();
            }
            File stdoutFile = result.content().stdout();
            System.out.format("%d bytes written to %s%n", stdoutFile.length(), stdoutFile);
            stdoutFile.delete();
            result.content().stderr().delete();
            // README_SNIPPET readme_example_launchAndCaptureFiles
        }

    }

    public static class Example_FeedStandardInputToProcess {
        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_feedStandardInput");
            // README_SNIPPET readme_example_feedStandardInput
            StreamInput input = StreamInput.fromFile(new File("/etc/passwd"));
            ProcessResult<String, String> result;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                ProcessMonitor<String, String> monitor = Subprocess.running("grep")
                        .arg("root")
                        .build()
                        .launcher(processTracker)
                        .outputStrings(Charset.defaultCharset(), input)
                        .launch();
                result = monitor.await();
            }
            System.out.println("grepped " + result.content().stdout());  // prints 'root' line from /etc/passwd
            // README_SNIPPET readme_example_feedStandardInput
        }

    }

    public static class Example_TerminateProcess {

        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_terminate");
            // README_SNIPPET readme_example_terminate
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                ProcessMonitor<String, String> monitor = Subprocess.running("cat")
                        .arg("-")
                        .build()
                        .launcher(processTracker)
                        .outputStrings(Charset.defaultCharset())
                        .launch();
                System.out.println("process alive? " + monitor.process().isAlive());
                SigtermAttempt attempt = monitor.destructor().sendTermSignal()
                        .await(3, TimeUnit.SECONDS);
                System.out.println("process alive? " + monitor.process().isAlive());
                if (monitor.process().isAlive()) {
                    attempt.kill().awaitOrThrow(3, TimeUnit.SECONDS);
                }
            }
            // README_SNIPPET readme_example_terminate
        }

    }

    public static class Example_CaptureProcessOutputInByteArrays {

        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_launchAndCaptureByteArrays");
            // README_SNIPPET readme_example_launchAndCaptureByteArrays
            ProcessResult<byte[], byte[]> result;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                ProcessMonitor<byte[], byte[]> monitor = Subprocess.running("echo")
                        .arg("foo")
                        .build()
                        .launcher(processTracker)
                        .outputInMemory()
                        .launch();
                result = monitor.await();
            }
            System.out.println(Arrays.toString(result.content().stdout())); // prints "[102, 111, 111, 10]"
            String stdoutText = new String(result.content().stdout(), US_ASCII);
            System.out.print(stdoutText); // prints "foo\n"
            // README_SNIPPET readme_example_launchAndCaptureByteArrays
        }

    }

    public static class Example_TailProcessOutput {

        public static void main(String[] args) throws Exception {
            System.out.println("readme_example_tailOutput");
            // README_SNIPPET readme_example_tailOutput
            PipedInputStream pipeInput = new PipedInputStream();
            StreamControl ctrl = new StreamControl() {
                @Override
                public OutputStream openStdoutSink() throws IOException {
                    return new PipedOutputStream(pipeInput);
                }

                @Override
                public OutputStream openStderrSink() {
                    return System.err; // in real life, wrap this with CloseShieldOutputStream to avoid closing JVM stderr
                }

                @Nullable
                @Override
                public InputStream openStdinSource() {
                    return null;
                }
            };
            StreamContext<StreamControl, Void, Void> ctx = new StreamContext<StreamControl, Void, Void>() {
                @Override
                public StreamControl produceControl() throws IOException {
                    return ctrl;
                }

                @Override
                public StreamContent<Void, Void> transform(int exitCode, StreamControl context) {
                    return StreamContent.absent();
                }
            };
            ProcessResult<Void, Void> result;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                // launch a process that prints a number every second
                ProcessMonitor<Void, Void> monitor = Subprocess.running("bash")
                        .arg("-c")
                        .arg("set -e; for N in $(seq 5) ; do sleep 1 ; echo $N ; done")
                        .build()
                        .launcher(processTracker)
                        .output(ctx)
                        .launch();
                // connect a reader to the piped input stream and echo the process output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pipeInput, US_ASCII))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("line: " + line);
                    }
                }
                result = monitor.await();
            }
            System.out.println("exit code: " + result.exitCode());
            // README_SNIPPET readme_example_tailOutput
        }

    }

}
