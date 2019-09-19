package io.github.mike10004.subprocess.test;

import com.google.common.base.CharMatcher;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ProcessTracker;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.ShutdownHookProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import io.github.mike10004.subprocess.test.Poller.PollOutcome;
import io.github.mike10004.subprocess.test.Poller.StopReason;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.rules.Timeout;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
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

    public static int getNumTrials() {
        return getSettingInt("subprocess.tests.trials", 1);
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

    private static <T> T parseIfDefined(@Nullable String token, Function<? super String, ? extends T> parser, @Nullable T valueIfUndefined) {
        if (!Strings.isNullOrEmpty(token)) {
            return parser.apply(token);
        }
        return valueIfUndefined;
    }

    public static int getSettingInt(String propertyName, int defaultValue) {
        return parseIfDefined(getSetting(propertyName), Integer::parseInt, defaultValue);
    }

    public static String getSetting(String propertyName) {
        String envVarName = propertyName.replaceAll("\\.", "_").toUpperCase();
        return getSetting(propertyName, envVarName);
    }

    private static String getSetting(String propertyName, String envVarName) {
        return getSetting(System::getProperty, propertyName, System::getenv, envVarName);
    }

    private static String getSetting(Function<String, String> sysprops, String propertyName, Function<String, String> env, String environmentVariableName) {
        String value = sysprops.apply(propertyName);
        if (value != null && !value.isEmpty()) { // check empty?
            return value;
        }
        return env.apply(environmentVariableName);
    }

    public static class Timeouts {

        public static final String PROPKEY_PREFIX = "subprocess.tests.timeout.";
        public static final String ENV_VAR_PREFIX = "SUBPROCESS_TESTS_TIMEOUT_";

        private static CharMatcher NUMBERS = CharMatcher.inRange('0', '9');
        private static CharMatcher LETTERS = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));

        private Timeouts(){}

        public static Duration get(Length length) {
            return length.duration();
        }

        public static Timeout rule(Duration duration) {
            return new Timeout(duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        public static Timeout briefRule() {
            return rule(brief());
        }

        /**
         * Synonym for {@link #brief()}.
         */
        public static Duration shorty() {
            return brief();
        }

        public static Duration brief() {
            return get(Length.SHORT);
        }

        public static Duration medium() {
            return get(Length.MEDIUM);
        }

        public static Timeout mediumRule() {
            return rule(medium());
        }

        public enum Length {

            SHORT(500), MEDIUM(5 * 1000), LONG(30 * 1000);

            private final long defaultMs;

            Length(long defaultMs) {
                this.defaultMs = defaultMs;
            }

            private String getSetting(Function<String, String> sysprops, Function<String, String> env) {
                String value = sysprops.apply(propertyName());
                if (value != null && !value.isEmpty()) { // check empty?
                    return value;
                }
                return env.apply(environmentVariableName());
            }

            public Duration duration() {
                return duration(System::getProperty, System::getenv);
            }

            public Duration duration(Function<String, String> sysprops, Function<String, String> env) {
                String definition = sysprops.apply(propertyName());
                if (definition != null && !definition.isEmpty()) {
                    String numbers = LETTERS.trimTrailingFrom(definition);
                    String unitToken = NUMBERS.trimLeadingFrom(definition);
                    TimeUnit unit = parseUnit(unitToken, TimeUnit.MILLISECONDS);
                    long magnitude = Long.parseLong(numbers);
                    long millis = unit.toMillis(magnitude);
                    return Duration.ofMillis(millis);
                }
                return Duration.ofMillis(defaultMs);
            }

            public final String environmentVariableName() {
                return ENV_VAR_PREFIX + name().toUpperCase();
            }

            public final String propertyName() {
                return PROPKEY_PREFIX + name().toLowerCase();
            }

            static TimeUnit parseUnit(String unitToken, TimeUnit defaultValue) {
                if (unitToken == null || unitToken.isEmpty()) {
                    return defaultValue;
                }
                unitToken = unitToken.toLowerCase();
                switch (unitToken) {
                    case "ms":
                    case "milli":
                    case "millis":
                    case "milliseconds":
                    case "millisecs":
                    case "millisec":
                        return TimeUnit.MILLISECONDS;
                    case "s":
                    case "sec":
                    case "seconds":
                        return TimeUnit.SECONDS;
                    case "m":
                    case "min":
                    case "minutes":
                        return TimeUnit.MINUTES;
                }
                throw new IllegalArgumentException("failed to parse unit: " + StringUtils.abbreviate(unitToken, 128));
            }
        }
    }
}
