package com.github.mike10004.nativehelper.subprocess.test;

import com.github.mike10004.nativehelper.subprocess.ProcessMonitor;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ProcessTracker;
import com.github.mike10004.nativehelper.subprocess.ScopedProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import java.io.File;
import java.nio.charset.Charset;

@SuppressWarnings("AccessStaticViaInstance")
public class ReadmeExamples {

    public static void main(String[] args) throws Exception {
        launchAndIgnoreOutput();
        launchAndCaptureOutputAsStrings();
        launchAndCaptureOutputInFiles();
        launchFeedingStdin();
    }

    public static void launchAndIgnoreOutput() throws Exception {
        System.out.println("launchAndIgnoreOutput");
        ProcessMonitor<?, ?> monitor = Subprocess.running("true").build()
                .launcher(ProcessTracker.create())
                .launch();
        ProcessResult<?, ?> result = monitor.await();
        System.out.println("exit with status " + result.exitCode());
    }

public static void launchAndCaptureOutputAsStrings() throws Exception {
    System.out.println("launchAndCaptureOutputAsStrings");
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
}

public static void launchAndCaptureOutputInFiles() throws Exception {
    System.out.println("launchAndCaptureOutputInFiles");
    ProcessMonitor<File, File> monitor = Subprocess.running("echo")
            .arg("this is in a file")
            .build()
            .launcher(ProcessTracker.create())
            .outputTempFiles(new File(System.getProperty("java.io.tmpdir")).toPath())
            .launch();
    ProcessResult<File, File> result = monitor.await();
    System.out.println("printed:");
    java.nio.file.Files.copy(result.content().stdout().toPath(), System.out);
}

public static void launchFeedingStdin() throws Exception {
    System.out.println("launchFeedingStdin");
    ByteSource input = Files.asByteSource(new File("/etc/passwd"));
    ProcessMonitor<String, String> monitor = Subprocess.running("grep")
            .arg("root")
            .build()
            .launcher(ProcessTracker.create())
            .outputStrings(Charset.defaultCharset(), input::openStream)
            .launch();
    ProcessResult<String, String> result = monitor.await();
    System.out.println("printed " + result.content().stdout());
}
}
