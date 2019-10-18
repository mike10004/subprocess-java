package io.github.mike10004.subprocess.test;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.io.Files;
import io.github.mike10004.nitsick.SettingSet;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.test.Poller.PollOutcome;
import io.github.mike10004.subprocess.test.Poller.StopReason;
import org.apache.commons.lang3.SystemUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class Tests {

    private Tests() {}

    public static final SettingSet Settings = SettingSet.global("subprocess.tests");

    public static int getNumTrials() {
        return Settings.get("trials", 1);
    }

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

    public static File pyEcho() {
        return getPythonFile("nht_echo.py");
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

    @Deprecated
    public static String readWhenNonempty(File file) throws InterruptedException {
        return readWhenNonempty(file, 1, US_ASCII);
    }

    /**
     * Waits until a file reaches a certain size and then reads its contents.
     * @param file the file
     * @param minimumByteLength number of bytes to wait for
     * @param charset file text encoding
     * @return contents of the file
     * @throws InterruptedException if thread sleeping between polls fails
     * @throws IllegalStateException if file does not reach the min size before timeout
     */
    public static String readWhenNonempty(File file, int minimumByteLength, Charset charset) throws InterruptedException {
        PollOutcome<String> outcome = new Poller<String>() {

            @Override
            protected PollAnswer<String> check(int pollAttemptsSoFar) {
                if (file.length() >= minimumByteLength) {
                    try {
                        return resolve(Files.asCharSource(file, charset).read());
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

    public static void dump(ProcessResult<?, ?> result, PrintStream out, PrintStream err) {
        String boundary = "=========================================================================";
        out.println(boundary);
        out.format("exit code %d%n", result.exitCode());
        out.println(boundary);
        out.println(boundary);
        out.print(nullToEmpty(result.content().stdout()));
        out.println(boundary);
        err.println(boundary);
        err.print(nullToEmpty(result.content().stderr()));
        err.println(boundary);
        err.println(boundary);
    }

    private static String nullToEmpty(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    private static boolean isSameFile(Path path, File file) throws IOException {
        return path.toFile().getCanonicalFile().equals(file.getCanonicalFile());
    }

    /**
     * Waits for a pidFile to become
     * @param pidFile
     * @throws InterruptedException
     * @throws IOException
     */
    public static int waitForSignalListenerPid(File pidFile) throws InterruptedException, IOException {
        @Nullable Integer pid = waitForFileChange(pidFile, path -> {
            String content;
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(path);
                content = new String(bytes, US_ASCII);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return null;
            }
            if (content.endsWith(System.lineSeparator())) {
                return Integer.parseInt(content.trim());
            }
            return null;
        });
        return pid.intValue();
    }

    public static <T> T waitForFileChange(File watchedFile, Function<Path, T> reader) throws InterruptedException, IOException {
        Path pidFileParent = watchedFile.getParentFile().toPath();
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            final WatchKey watchKey = pidFileParent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                final WatchKey wk = watchService.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    //we only register "ENTRY_MODIFY" so the context is always a Path.
                    Path changed = (Path) event.context();
                    changed = pidFileParent.resolve(changed);
                    System.out.format("file changed: %s%n", changed);
                    if (isSameFile(changed, watchedFile)) {
                        T content = reader.apply(changed);
                        if (content != null) {
                            return content;
                        }
                    } else System.err.format("not the same file (expect %s)%n", watchedFile);
                }
                wk.reset();
            }
        }
    }
}
