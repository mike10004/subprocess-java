package io.github.mike10004.subprocess;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.mike10004.nitsick.junit.TimeoutRules;
import io.github.mike10004.subprocess.test.ListenableSubprocessLauncher;
import io.github.mike10004.subprocess.test.Tests;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomExecutorServiceTest {

    @Rule
    public final Timeout timeout = TimeoutRules.from(Tests.Settings).getLongRule();

    @ClassRule
    public static final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testCustomExecutorService() throws Exception {
        File pidFile = File.createTempFile("nht_pid_file", ".pid", tempFolder.newFolder());
        Executor directExecutor = MoreExecutors.directExecutor();
        AtomicInteger callbackInvocations = new AtomicInteger(0);
        ProcessResult<?, ?> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            Subprocess subprocess = Tests.runningPythonFile(Tests.pySignalListener())
                    .args("--pidfile", pidFile.getAbsolutePath())
                    .build();
            Supplier<ListeningExecutorService> executorServiceSupplier = ExecutorServices.newTransformedSingleThreadExecutorServiceFactory("unit-test-pool", MoreExecutors::listeningDecorator);
            ProcessMonitor<?, ?> monitor = subprocess.launcher(new ListenableSubprocessLauncher(processTracker, executorServiceSupplier))
                .outputStrings(Charset.defaultCharset())
                .launch();
            System.out.format("launched %s%nwaiting for pid to appear in %s%n", subprocess, pidFile);
            int pid = Tests.waitForSignalListenerPid(pidFile);
            System.out.format("saw pid %d%n", pid);
            Future<?> future = monitor.future();
            assertTrue("expect is ListenableFuture: " + future, future instanceof ListenableFuture);
            ListenableFuture<?> lFuture = (ListenableFuture<?>) future;
            Futures.addCallback(lFuture, new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object result) {
                    always(result, null);
                }

                @Override
                public void onFailure(Throwable t) {
                    always(null, t);
                }

                private void always(@Nullable Object result, @Nullable Throwable failure) {
                    callbackInvocations.incrementAndGet();
                    System.out.format("thread=%s: result=%s; failure=%s%n", Thread.currentThread().getName(), result, failure);
                }

            }, directExecutor);
            monitor.destructor().sendTermSignal().await(5, TimeUnit.SECONDS);
            result = monitor.await(0, TimeUnit.SECONDS);
        }
        assertEquals("expect callback invoked exactly once", 1, callbackInvocations.get());
        Tests.dump(result, System.out, System.out);
    }

}
