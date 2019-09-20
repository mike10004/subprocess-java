package io.github.mike10004.subprocess;

import com.google.common.util.concurrent.Futures;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SubprocessLaunchSupportTest {

    @Test
    public void map() throws Exception {
        Subprocess p = Subprocess.running("echo").arg("hello, world").build();
        StreamControl ctrl = new StreamContexts.BucketContext(null);
        StreamContext<?, String, String> ctx = StreamContexts.predefined(ctrl, () -> "5", () -> "true");
        SubprocessLauncher launcher = new BasicSubprocessLauncher(EasyMock.createMock(ProcessTracker.class));
        SubprocessLaunchSupport<Integer, Boolean> launchSupport = new SubprocessLaunchSupport<String, String>(p, launcher, ctx) {
            @Override
            public ProcessMonitor<String, String> launch() throws SubprocessException {
                return new CannedMonitor<>(ProcessResult.direct(0, "5", "true"));
            }
        }.map(Integer::valueOf, Boolean::parseBoolean);
        ProcessResult<Integer, Boolean> result = launchSupport.launch().await();
        assertEquals(5, result.content().stdout().intValue());
        assertTrue(result.content().stderr());
    }

    private static class CannedMonitor<SO, SE> implements ProcessMonitor<SO,SE> {

        private final ProcessResult<SO, SE> result;

        private CannedMonitor(ProcessResult<SO, SE> result) {
            this.result = result;
        }

        @Override
        public Future<ProcessResult<SO, SE>> future() {
            return Futures.immediateFuture(result);
        }

        @Override
        public ProcessDestructor destructor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Process process() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessTracker tracker() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessResult<SO, SE> await(long timeout, TimeUnit unit) {
            return result;
        }

        @Override
        public ProcessResult<SO, SE> await() throws SubprocessException {
            return result;
        }
    }
}