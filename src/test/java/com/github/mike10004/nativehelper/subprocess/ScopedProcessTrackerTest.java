package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.test.Tests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ScopedProcessTrackerTest {

    private static final int NUM_TRIALS = 50;

    private final int trial;

    public ScopedProcessTrackerTest(int trial) {
        this.trial = trial;
    }

    @Parameters
    public static List<Integer> trials() {
        List<Integer> trials = new ArrayList<>();
        for (int i = 0; i < NUM_TRIALS; i++) {
            trials.add(i);
        }
        return trials;
    }

    private static final Set<Integer> exitCodes = new HashSet<>();

    @Test
    public void close() throws Exception {
        ProcessMonitor<?, ?> monitor;
        try (ScopedProcessTracker tracker = new ScopedProcessTracker()) {
            Subprocess subprocess = createSubprocessThatWaitsUntilDestroyed();
            monitor = subprocess.launcher(tracker).launch();
        }
        assertFalse("isAlive", monitor.process().isAlive());
        ProcessResult<?, ?> result = monitor.await(0, TimeUnit.MILLISECONDS);
        if (exitCodes.add(result.exitCode())) {
            System.out.format("[%d] exit code %d%n", trial, result.exitCode());
        }
        assertNotEquals("exit code", 0, result.exitCode());
    }

    private static Subprocess createSubprocessThatWaitsUntilDestroyed() {
        return Tests.runningPythonFile(Tests.pySignalListener()).build();
    }
}