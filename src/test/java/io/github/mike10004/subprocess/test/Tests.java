package io.github.mike10004.subprocess.test;

import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.test.Poller.PollOutcome;
import io.github.mike10004.subprocess.test.Poller.StopReason;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.io.Files;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class Tests {

    private Tests() {}

    public static File getTestSourcesDir() {
        return new File(getProperties().getProperty("project.basedir"), "src/test");
    }

    public static File getPythonTestSourcesDir() {
        File f = new File(getTestSourcesDir(), "python");
        checkState(f.isDirectory(), "not a directory: %s", f);
        return f;
    }

    public static File getPythonFile(String relativePath) {
        return getPythonTestSourcesDir().toPath().resolve(relativePath).toFile();
    }

    private static final Supplier<Properties> propertiesSupplier = Suppliers.memoize(() -> {
        Properties p = new Properties();
        String resourcePath = "/test.properties";
        try (InputStream in = Tests.class.getResourceAsStream(resourcePath)) {
            checkState(in != null, "not found: classpath:" + resourcePath);
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkState(!p.isEmpty(), "no properties loaded");
        return p;
    });

    public static Properties getProperties() {
        return propertiesSupplier.get();
    }

    public static File pyReadInput() {
        return getPythonFile("nht_read_input.py");
    }

    public static String joinPlus(String delimiter, Iterable<String> items) {
        return String.join(delimiter, items) + delimiter;
    }

    public static File pyCat() {
        return getPythonFile("nht_cat.py");
    }

    public static File pySignalListener() {
        return getPythonFile("nht_signal_listener.py");
    }

    public static Subprocess.Builder runningPythonFile(String name) {
        File pythonFile = getPythonFile(name);
        return runningPythonFile(pythonFile);
    }

    public static Subprocess.Builder runningPythonFile(File pythonFile) {
        if (isPlatformWindows()) {
            return getPython3BuilderChecked()
                    .arg(pythonFile.getAbsolutePath());
        } else {
            checkArgument(pythonFile.canExecute(), "not executable: %s", pythonFile);
            return Subprocess.running(pythonFile);
        }
    }

    private static Subprocess.Builder getPython3Builder() {
        String pythonEnvValue = System.getenv("PYTHON");
        if (Strings.isNullOrEmpty(pythonEnvValue)) {
            return Subprocess.running("python");
        } else {
            File pythonExecutable = new File(pythonEnvValue, "python.exe");
            return Subprocess.running(pythonExecutable);
        }
    }

    private static Subprocess.Builder getPython3BuilderChecked() {
        checkState(isPlatformWindows(), "this oughta be windows; if not, the file should be chmod'd as executable and executed as argv[0]");
        checkState(isPython3BuilderReallyPython3.get(), "python 3.x must be the version that bare `python` executes");
        return getPython3Builder();
    }

    // from __future__ import print_function; import sys; print(sys.version_info[0], end="");
    private static Supplier<Boolean> isPython3BuilderReallyPython3 = Suppliers.memoize(() -> {
        Subprocess subprocess = getPython3Builder()
                .arg("-c")
                .arg("from __future__ import print_function; import sys; print(sys.version_info[0]);")
                .build();
        ProcessResult<String, String> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = subprocess.launcher(processTracker)
                    .outputStrings(Charset.defaultCharset())
                    .launch()
                    .await(5, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        if (result.exitCode() != 0) {
            System.err.format("exit %d%n", result.exitCode());
            System.err.println(result.content().stderr());
            throw new IllegalStateException("nonzero exit from python");
        }
        String majorVersion = result.content().stdout().trim();
        if (!"3".equals(majorVersion)) {
            System.err.format("actual major version: %s%n", majorVersion);
        }
        return "3".equals(majorVersion);
    });

    public static String readWhenNonempty(File file) throws InterruptedException {
        PollOutcome<String> outcome = new Poller<String>() {

            @Override
            protected PollAnswer<String> check(int pollAttemptsSoFar) {
                if (file.length() > 0) {
                    try {
                        return resolve(Files.asCharSource(file, StandardCharsets.US_ASCII).read());
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                        return abortPolling();
                    }
                }
                return continuePolling();
            }
        }.poll(250, 20);
        if (outcome.reason == StopReason.RESOLVED) {
            return outcome.content;
        }
        throw new IllegalStateException("polling for nonempty file failed: " + file);
    }

    public static boolean isPlatformWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public static boolean isPlatformLinux() {
        return SystemUtils.IS_OS_LINUX;
    }
}
