package io.github.mike10004.subprocess.test;

import io.github.mike10004.subprocess.ProcessTracker;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

public class ProcessTrackerRule extends ExternalResource {

    private final TestWatcher watcher;
    private final AtomicBoolean passage;
    private ProcessTracker processTracker;

    public ProcessTrackerRule() {
        passage = new AtomicBoolean(false);
        watcher = new TestWatcher() {
            @Override
            protected void succeeded(Description description) {
                passage.set(true);
            }
        };
    }

    @Override
    public Statement apply(Statement base, Description description) {
        watcher.apply(base, description);
        return super.apply(base, description);
    }

    @Override
    protected void before() {
        processTracker = ProcessTracker.create();
    }

    @Override
    protected void after() {
        ProcessTracker processContext = this.processTracker;
        if (processContext != null) {
            boolean testPassed = passage.get();
            if (testPassed) {
                if (processContext.activeCount() > 0) {
                    System.err.format("%d active processes in context%n", processContext.activeCount());
                }
            } else {
                assertEquals("number of active processes in context must be zero", 0, processContext.activeCount());
            }
        }
    }

    public ProcessTracker getTracker() {
        return processTracker;
    }
}
