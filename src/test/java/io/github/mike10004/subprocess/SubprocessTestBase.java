package io.github.mike10004.subprocess;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

/**
 * Base class for subprocess tests. This superclass provides a {@link ProcessTracker} field
 * for test classes to use, and implements a check after each test that confirms that there
 * are no active processes remaining.
 */
@RunWith(Parameterized.class)
public abstract class SubprocessTestBase {

    protected static final ProcessTracker TRACKER = ProcessTracker.create();

    private volatile boolean testFailed;
    private volatile Description description;

    @Rule
    public final TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            SubprocessTestBase.this.description = description;
        }

        @Override
        protected void failed(Throwable e, Description description) {
            testFailed = true;
        }
    };

    private ShutdownHookProcessTracker getTracker() {
        return (ShutdownHookProcessTracker) TRACKER;
    }

    @After
    public final void checkProcesses() {
        try {
            int active = getTracker().activeCount();
            if (testFailed) {
                if (active > 0) {
                    System.err.format("%d active processes; ignoring because test failed%n", active);
                }
            } else {
                assertEquals(active + " processes are still active but should have finished or been killed after " + description, 0, active);
            }
        } catch (RuntimeException e) {
            System.err.println("SubprocessTestBase.checkProcesses runtime exception");
            e.printStackTrace(System.err);
            throw e;
        } finally {
            ((ShutdownHookProcessTracker) TRACKER).destroyAll(5, TimeUnit.SECONDS);
        }
    }

    private static int getNumTrials() {
        String trialsStr = System.getenv("SUBPROCESS_TESTS_TRIALS");
        if (trialsStr != null && !trialsStr.isEmpty()) {
            try {
                return Integer.parseInt(trialsStr);
            } catch (RuntimeException e) {
                System.err.format("failed to parse %s: %s%n", trialsStr, e);
            }
        }
        return 1;
    }

    @Parameters
    public static List<Object[]> params() {
        int numTrials = getNumTrials();
        return Arrays.asList(new Object[numTrials][0]);
    }

    protected static <T> Supplier<T> nullSupplier() {
        return () -> null;
    }

}
