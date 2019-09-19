package io.github.mike10004.subprocess;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import io.github.mike10004.subprocess.test.Tests;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public class SubprocessTest extends SubprocessTestBase {

    public SubprocessTest(int trial) {
        super(trial);
    }

    @Test
    public void launch_true() throws Exception {
        int exitCode = Tests.runningPythonFile(pyTrue()).build()
                .launcher(TRACKER)
                .launch().await().exitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void launch_exit_3() throws Exception {
        int expected = 3;
        int exitCode = Tests.runningPythonFile(pyExit())
                .arg(String.valueOf(expected))
                .build()
                .launcher(TRACKER)
                .launch().await().exitCode();
        assertEquals("exit code", expected, exitCode);
    }

    private static File pyEcho() {
        return Tests.getPythonFile("nht_echo.py");
    }

    private static File pyTrue() {
        return Tests.getPythonFile("nht_true.py");
    }

    private static File pyExit() {
        return Tests.getPythonFile("nht_exit.py");
    }

    @Test
    public void launch_echo() throws Exception {
        String arg = "hello";
        ProcessResult<String, String> processResult = Tests.runningPythonFile(pyEcho())
                .arg(arg).build()
                .launcher(TRACKER)
                .outputStrings(US_ASCII)
                .launch().await();
        int exitCode = processResult.exitCode();
        assertEquals("exit code", 0, exitCode);
        String actualStdout = processResult.content().stdout();
        String actualStderr = processResult.content().stderr();
        System.out.format("output: \"%s\"%n", StringEscapeUtils.escapeJava(actualStdout));
        assertEquals("stdout", arg, actualStdout);
        assertEquals("stderr", "", actualStderr);
    }

    @Test
    public void launch_stereo() throws Exception {
        List<String> stdout = Arrays.asList("foo", "bar", "baz"), stderr = Arrays.asList("gaw", "gee");
        List<String> args = new ArrayList<>();
        for (int i = 0; i < Math.max(stdout.size(), stderr.size()); i++) {
            if (i < stdout.size()) {
                args.add(stdout.get(i));
            }
            if (i < stderr.size()) {
                args.add(stderr.get(i));
            }
        }
        ProcessResult<String, String> result =
                Tests.runningPythonFile(Tests.getPythonFile("nht_stereo.py"))
                .args(args)
                .build()
                .launcher(TRACKER)
                .outputStrings(US_ASCII)
                .launch().await();
        String actualStdout = result.content().stdout();
        String actualStderr = result.content().stderr();
        assertEquals("stdout", Tests.joinPlus(linesep, stdout), actualStdout);
        assertEquals("stderr", Tests.joinPlus(linesep, stderr), actualStderr);
    }

    private static final String linesep = System.lineSeparator();

    @Test
    public void launch_cat_stdin() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 2 * 1024 * 1024; // overwhelm StreamPumper's buffer
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        ProcessResult<byte[], byte[]> result =
                Tests.runningPythonFile(Tests.pyCat())
                        .build()
                        .launcher(TRACKER)
                        .outputInMemory(StreamInput.wrap(bytes))
                        .launch().await();
        System.out.println(result);
        assertEquals("exit code", 0, result.exitCode());
        assertArrayEquals("stdout", bytes, result.content().stdout());
        assertEquals("stderr length", 0, result.content().stderr().length);
    }

    @Test
    public void launch_cat_file() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 256 * 1024;
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        File dataFile = File.createTempFile("SubprocessTest", ".dat");
        Files.asByteSink(dataFile).write(bytes);
        ProcessResult<byte[], byte[]> result =
                Tests.runningPythonFile(Tests.pyCat())
                        .arg(dataFile.getAbsolutePath())
                        .build()
                        .launcher(TRACKER)
                        .outputInMemory(StreamInput.wrap(bytes))
                        .launch().await();
        System.out.println(result);
        assertEquals("exit code", 0, result.exitCode());
        checkState(Arrays.equals(bytes, Files.asByteSource(dataFile).read()));
        assertArrayEquals("stdout", bytes, result.content().stdout());
        assertEquals("stderr length", 0, result.content().stderr().length);
        //noinspection ResultOfMethodCallIgnored
        dataFile.delete();
    }

    @Test
    public void launch_readInput_predefined() throws Exception {
        String expected = String.format("foo%nbar%n");
        ProcessResult<String, String> result = Tests.runningPythonFile(Tests.pyReadInput())
                .build()
                .launcher(TRACKER)
                .outputStrings(US_ASCII, CharSource.wrap(expected + System.lineSeparator()).asByteSource(US_ASCII)::openStream)
                .launch().await();
        System.out.println(result);
        assertEquals("output", expected, result.content().stdout());
        assertEquals("exit code", 0, result.exitCode());
    }

    /**
     * Runs echo, printing to the JVM stdout stream.
     * This is not an automated test, as it requires visual inspection in the console
     * to confirm expected behavior, but it's useful for diagnosing issues.
     */
    @Test
    public void launch_echo_inherit() throws Exception {
        int exitCode = Tests.runningPythonFile(pyEcho())
                .arg("hello, world")
                .build()
                .launcher(TRACKER)
                .inheritOutputStreams()
                .launch().await().exitCode();
        checkState(exitCode == 0);
        System.out.println();
        System.out.flush();
    }

    @Test
    public void launch_stereo_inherit() throws Exception {
        PrintStream JVM_STDOUT = System.out, JVM_STDERR = System.err;
        ByteBucket stdoutBucket = ByteBucket.create(), stderrBucket = ByteBucket.create();
        ProcessResult<Void, Void> result;
        try (PrintStream tempOut = new PrintStream(stdoutBucket.openStream(), true);
            PrintStream tempErr = new PrintStream(stderrBucket.openStream(), true)) {
            System.setOut(tempOut);
            System.setErr(tempErr);
            result = Tests.runningPythonFile(Tests.getPythonFile("nht_stereo.py"))
                    .args("foo", "bar")
                    .build()
                    .launcher(TRACKER)
                    .inheritOutputStreams()
                    .launch().await();
            assertEquals("exit code", 0, result.exitCode());
        } finally {
            System.setOut(JVM_STDOUT);
            System.setErr(JVM_STDERR);
        }
        System.out.format("result: %s%n", result);
        String actualStdout = stdoutBucket.decode(US_ASCII);
        String actualStderr = stderrBucket.decode(US_ASCII);
        assertEquals("stdout", "foo" + System.lineSeparator(), actualStdout);
        assertEquals("stderr", "bar" + System.lineSeparator(), actualStderr);
    }

    private static CharMatcher alphanumeric() {
        return CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.inRange('0', '9'));
    }

    @Test(expected = ProcessLaunchException.class)
    public void launch_notAnExecutable() throws Exception {
        String executable  = "e" + alphanumeric().retainFrom(UUID.randomUUID().toString());
        Subprocess.running(executable).build().launcher(TRACKER).launch();
    }

    private static final String homeVarName;
    private static final String userVarName;

    /**
     * Returns the first environment variable name that is a case-insensitive match for the given string.
     * Windows has super weird ways of dealing with the environment as supplied by the envp
     * argument to {@link Runtime#exec(String, String[])}. Under almost all circumstances,
     * when retrieving values corresponding to environment variable names, a case-insensitive
     * name match is performed. However, when you supply additional variable definitions with
     * {@link Runtime#exec(String, String[])}, inherited variables with names that are case-insensitively
     * equal are not overridden; the alternate case-mismatching definition is appended to the
     * environment. For a given build environment, to correctly override an inherited variable,
     * we first have to find the exact match for its name.
     */
    private static String matchCaseInsensitiveEnvVariableName(String varname) {
        return System.getenv().keySet().stream().filter(varname::equalsIgnoreCase).findFirst().orElseThrow(() -> new IllegalStateException("no match for " + varname));
    }

    static {
        if (Tests.isPlatformWindows()) {
            homeVarName = matchCaseInsensitiveEnvVariableName("USERPROFILE");
            userVarName = matchCaseInsensitiveEnvVariableName("USERNAME");
        } else {
            homeVarName = "HOME";
            userVarName = "USER";
        }
    }
    @Test
    public void launch_env_noSupplements() throws Exception {
        String expectedUser = System.getProperty("user.name");
        String expectedHome = System.getProperty("user.home");
        Map<String, String> result = launchEnv(ImmutableMap.of(), userVarName, homeVarName);
        assertCongruent(result, userVarName, homeVarName);
        assertEquals(userVarName, expectedUser, result.get(userVarName));
        assertEquals(homeVarName, expectedHome, result.get(homeVarName));
    }

    @Test
    public void launch_env_override() throws Exception {
        String expectedUser = System.getProperty("user.name");
        String expectedHome = FileUtils.getTempDirectory().getAbsolutePath();
        Map<String, String> result = launchEnv(ImmutableMap.of(homeVarName, expectedHome), userVarName, homeVarName);
        assertCongruent(result, userVarName, homeVarName);
        assertEquals(userVarName, expectedUser, result.get(userVarName));
        assertEquals(homeVarName, expectedHome, result.get(homeVarName));
    }

    @Test
    public void launch_env_withSupplements() throws Exception {
        String expectedUser = System.getProperty("user.name");
        String expectedHome = System.getProperty("user.home");
        String expectedFoo = "bar";
        Map<String, String> result = launchEnv(ImmutableMap.of("FOO", expectedFoo), userVarName, homeVarName, "FOO");
        assertCongruent(result, userVarName, homeVarName, "FOO");
        assertEquals(userVarName, expectedUser, result.get(userVarName));
        assertEquals(homeVarName, expectedHome, result.get(homeVarName));
        assertEquals("FOO", expectedFoo, result.get("FOO"));
    }

    private static void assertCongruent(Map<String, String> printedEnv, String...varnamesArray) {
        assertEquals("variable names", ImmutableSet.copyOf(varnamesArray), printedEnv.keySet());
    }

    private static final Splitter NHT_ENV_SPLITTER = Splitter.on('=').limit(2);

    private Map<String, String> launchEnv(Map<String, String> env, String...varnamesArray) throws InterruptedException {
        List<String> varnames = Arrays.asList(varnamesArray);
        String prog = "nht_env.py";
        Subprocess subprocess = Tests.runningPythonFile(Tests.getPythonFile(prog))
                .env(env)
                .args(varnames)
                .build();
        ProcessResult<String, String> result = subprocess.launcher(TRACKER)
                .outputStrings(Charset.defaultCharset())
                .launch().await();
        System.out.format("%s exited %s%n", prog, result.exitCode());
        System.out.format("%s stdout:%n%s%n", prog, result.content().stdout());
        System.out.format("%s stderr:%n%s%n", prog, result.content().stderr());
        List<String> lines = splitLines(result.content().stdout());
        assertEquals("exit code", 0, result.exitCode());
        return lines.stream().map(line -> Iterables.toArray(NHT_ENV_SPLITTER.split(line), String.class))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }

    private static List<String> splitLines(String text) {
        return Splitter.on(System.lineSeparator()).omitEmptyStrings().splitToList(text);
    }

    @Test
    public void test_splitLines() {
        String line = "C:\\Users\\jgarner";
        String text = line + System.lineSeparator();
        System.out.format("text = \"%s\"%n", StringEscapeUtils.escapeJava(text));
        List<String> lines = splitLines(text);
        assertEquals("lines", ImmutableList.of(line), lines);
    }

    @Test
    public void killRemovesFromTracker() throws Exception {
        Map<Process, Boolean> processes = Collections.synchronizedMap(new HashMap<>());
        AtomicBoolean neverTracked = new AtomicBoolean(false);
        ProcessTracker localContext = new ProcessTracker() {
            @Override
            public synchronized void add(Process process) {
                System.out.format("localContext.add(%s)%n", process);
                processes.put(process, true);
            }

            @Override
            public synchronized boolean remove(Process process) {
                Boolean previous = processes.put(process, false);
                if (previous == null) {
                    System.out.format("localContext.remove(%s) (never tracked)%n", process);
                    neverTracked.set(true);
                    return false;
                }
                boolean alreadyRemoved = !previous.booleanValue();
                System.out.format("localContext: remove %s (already removed? %s)%n", process, alreadyRemoved);
                return !alreadyRemoved;
            }

            @Override
            public synchronized int activeCount() {
                return Ints.checkedCast(processes.entrySet().stream().filter(Entry::getValue).count());
            }

        };
        ProcessMonitor<?, ?> monitor = Tests.runningPythonFile(Tests.pySignalListener())
                .build().launcher(localContext).launch();
        System.out.format("killRemovesFromTracker: %s%n", monitor.process());
        ProcessResult<?, ?> result = null;
        try {
            result = monitor.await(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignore) {
        }
        checkState(result == null, "result should be null but isn't");
        DestroyAttempt term = monitor.destructor().sendTermSignal().await();
        checkState(DestroyResult.TERMINATED == term.result());
        Set<Process> pset = ImmutableSet.copyOf(processes.keySet());
        checkState(pset.size() == 1);
        Process process = pset.iterator().next();
        checkState(!process.isAlive(), "process still alive");
        assertEquals("active count", 0, localContext.activeCount());
        assertEquals("neverTracked", false, neverTracked.get());
    }
}