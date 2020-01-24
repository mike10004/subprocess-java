package io.github.mike10004.subprocess.test;

import io.github.mike10004.subprocess.BasicSubprocessLauncher;
import io.github.mike10004.subprocess.StreamTailer;
import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.SigtermAttempt;
import io.github.mike10004.subprocess.StreamInput;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.SubprocessLauncher;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
            Consumer<String> filter = line -> {
                if (Integer.parseInt(line) % 2 == 0) {
                    System.out.print(line + " ");
                }
            };
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                StreamTailer tailer = StreamTailer.builder(Charset.defaultCharset())
                        .stdoutConsumer(filter)
                        .build();
                // launch a process that prints a number every second
                ProcessMonitor<Void, Void> monitor = Subprocess.running("bash")
                        .arg("-c")
                        .arg("set -e; for N in $(seq 5) ; do sleep 0.5 ; echo $N ; done")
                        .build()
                        .launcher(processTracker)
                        .tailing(executorService, tailer)
                        .launch();
                monitor.await();
            }
            // prints: 2 4
            executorService.shutdown();
            // README_SNIPPET readme_example_tailOutput
            System.out.println();
        }

    }

}
