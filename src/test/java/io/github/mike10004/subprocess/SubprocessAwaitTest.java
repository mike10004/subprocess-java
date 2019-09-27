package io.github.mike10004.subprocess;

import io.github.mike10004.nitsick.junit.TimeoutRules;
import io.github.mike10004.subprocess.test.Tests;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SubprocessAwaitTest extends SubprocessTestBase {

    @Rule
    public final Timeout timeout = TimeoutRules.from(Tests.Settings).getMediumRule();

    public SubprocessAwaitTest(int trial) {
        super(trial);
    }

    @Test
    public void awaitWithTimeout() throws Exception {
        try {
            System.out.println("awaitWithTimeout");
            ProcessMonitor<?, ?> monitor = Tests.runningPythonFile(Tests.pySignalListener())
                    .build().launcher(TRACKER).launch();
            ProcessResult<?, ?> result = null;
            try {
                result = monitor.await(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ignore) {
            }
            assertNull("result should not have been assigned", result);
            DestroyAttempt term = monitor.destructor().sendTermSignal().await();
            assertEquals("term result", DestroyResult.TERMINATED, term.result());
        } catch (UnsupportedOperationException e) {
            System.err.println("awaitWithTimeout: UnsupportedOperationException");
            e.printStackTrace(System.err);
            throw e;
        }
    }

}
