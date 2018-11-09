package io.github.mike10004.subprocess.test;

import io.github.mike10004.subprocess.DestroyAttempt;
import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ProcessTracker;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.StreamInput;
import io.github.mike10004.subprocess.Subprocess;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class ReadmeExamples {

    public static void main(String[] args) throws Exception {
        Example1.main(args);
        Example2.main(args);
        Example3.main(args);
        Example4.main(args);
        Example5.main(args);
    }

    public static class Example1 {

        public static void main(String[] args) throws Exception {
            System.out.println("launchAndIgnoreOutput");
            ProcessMonitor<?, ?> monitor = Subprocess.running("true").build()
                    .launcher(ProcessTracker.create())
                    .launch();
            ProcessResult<?, ?> result = monitor.await();
            System.out.println("exit with status " + result.exitCode());
        }
    }

    public static class Example2 {

        public static void main(String[] args) throws Exception {
            System.out.println("launchAndCaptureOutputAsStrings");
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
    public static class Example3 {
        public static void main(String[] args) throws Exception {
            System.out.println("launchAndCaptureOutputInFiles");
            // README_SNIPPET readme_example_launchAndCaptureFiles
            ProcessMonitor<File, File> monitor = Subprocess.running("echo")
                    .arg("0123456789")
                    .build()
                    .launcher(ProcessTracker.create())
                    .outputTempFiles(new File(System.getProperty("java.io.tmpdir")).toPath())
                    .launch();
            ProcessResult<File, File> result = monitor.await();
            File stdoutFile = result.content().stdout();
            System.out.format("%d bytes written to %s%n", stdoutFile.length(), stdoutFile);
            stdoutFile.delete();
            result.content().stderr().delete();
            // README_SNIPPET readme_example_launchAndCaptureFiles
        }

    }

    public static class Example4 {
        public static void main(String[] args) throws Exception {
            System.out.println("launchFeedingStdin");
            // README_SNIPPET readme_example_feedStandardInput
            StreamInput input = StreamInput.fromFile(new File("/etc/passwd"));
            ProcessMonitor<String, String> monitor = Subprocess.running("grep")
                    .arg("root")
                    .build()
                    .launcher(ProcessTracker.create())
                    .outputStrings(Charset.defaultCharset(), input)
                    .launch();
            ProcessResult<String, String> result = monitor.await();
            System.out.println("printed " + result.content().stdout());
            // README_SNIPPET readme_example_feedStandardInput
        }

    }

    public static class Example5 {

        public static void main(String[] args) throws Exception {
            // README_SNIPPET readme_example_terminate
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                ProcessMonitor<String, String> monitor = Subprocess.running("cat")
                        .arg("-")
                        .build()
                        .launcher(processTracker)
                        .outputStrings(Charset.defaultCharset())
                        .launch();
                System.out.println("process alive? " + monitor.process().isAlive());
                DestroyAttempt.TermAttempt attempt = monitor.destructor().sendTermSignal()
                        .timeout(3, TimeUnit.SECONDS);
                System.out.println("process alive? " + monitor.process().isAlive());
                if (monitor.process().isAlive()) {
                    attempt.kill().timeoutOrThrow(3, TimeUnit.SECONDS);
                }
            }
            // README_SNIPPET readme_example_terminate
        }

    }

}
