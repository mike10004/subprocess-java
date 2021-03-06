package io.github.mike10004.subprocess;

import io.github.mike10004.nitsick.junit.TimeoutRules;
import io.github.mike10004.subprocess.test.Tests;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.File;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

public class SubprocessKillTest extends SubprocessTestBase {

    @Rule
    public final Timeout timeout = TimeoutRules.from(Tests.Settings).getMediumRule();

    public SubprocessKillTest(int trial) {
        super(trial);
    }

    private static Subprocess signalProgram(boolean swallowSigterm, File pidFile) {
        Subprocess.Builder builder = Tests.runningPythonFile(Tests.pySignalListener());
        builder.args("--pidfile", pidFile.getAbsolutePath());
        if (swallowSigterm) {
            builder.arg("--swallow-sigterm");
        }
        return builder.build();
    }

    @Test
    public void destroyWithSigKill() throws Exception {
        /*
         * Normally, we run our nht_signal_listener.py program here and specify that it should swallow SIGTERM, which
         * prevents Process.destroy from terminating the process, and then we confirm that behavior and subsequently
         * confirm that Process.destroyForcibly terminates the process. On Windows, the Process.destroy call works
         * right away. I'm not sure whether that's a Windows thing or we need to change nht_signal_listener, but
         * anyway it causes this test to fail. In order to test this properly, we have to create some Windows process
         * that quietly consumes the Process.destroy call. Until we do that, we'll just ignore this and hope that
         * all Process.destroy calls succeed for code running on Windows.
         */
        Assume.assumeFalse("ignore this test on Windows because Process.destroy kills our signal_listener program", Tests.isPlatformWindows());
        File pidFile = File.createTempFile("SubprocessKillTest", ".pid");
        ProcessMonitor<?, ?> monitor = signalProgram(true, pidFile)
                .launcher(TRACKER)
                .inheritOutputStreams()
                .launch();
        System.out.println("waiting for pid to be printed...");
        String pid = Tests.readWhenNonempty(pidFile, 1, US_ASCII);
        System.out.format("pid printed: %s%n", pid);
        SigtermAttempt termAttempt = monitor.destructor().sendTermSignal();
        assertEquals("should still be alive after SIGTERM", DestroyResult.STILL_ALIVE, termAttempt.result());
        SigkillAttempt attempt = termAttempt.kill();
        attempt.awaitKill();
        int exitCode = monitor.await().exitCode();
        if (Tests.isPlatformLinux()) {
            assertEquals("exit code", EXPECTED_SIGKILL_EXIT_CODE, exitCode);
        }
    }

    @Test
    public void destroyWithSigTerm() throws Exception {
        File pidFile = File.createTempFile("SubprocessKillTest", ".pid");
        ProcessMonitor<?, ?> monitor = signalProgram(false, pidFile)
                .launcher(TRACKER)
                .inheritOutputStreams()
                .launch();
        System.out.println("waiting for pid to be printed...");
        String pid = Tests.readWhenNonempty(pidFile, 1, US_ASCII);
        System.out.format("pid printed: %s%n", pid);
        SigtermAttempt termAttempt = monitor.destructor().sendTermSignal().await();
        assertEquals("term attempt result", DestroyResult.TERMINATED, termAttempt.result());
        int exitCode = monitor.await().exitCode();
        if (Tests.isPlatformLinux()) {
            assertEquals("exit code", EXPECTED_SIGTERM_EXIT_CODE, exitCode);
        }
    }

    private static final int EXPECTED_SIGTERM_EXIT_CODE = 128 + 15;
    private static final int EXPECTED_SIGKILL_EXIT_CODE = 128 + 9;

}
