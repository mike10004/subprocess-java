package io.github.mike10004.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

class DestroyAttempts {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(DestroyAttempts.class);

    private static final class AlreadyTerminated implements DestroyAttempt.KillAttempt, DestroyAttempt.TermAttempt {
        @Override
        public DestroyResult result() {
            return DestroyResult.TERMINATED;
        }

        @Override
        public TermAttempt await() {
            return this;
        }

        @Override
        public TermAttempt timeout(long duration, TimeUnit unit) {
            return this;
        }

        @Override
        public void awaitKill() {
        }

        @Override
        public boolean timeoutKill(long duration, TimeUnit timeUnit) {
            return true;
        }

        @Override
        public void timeoutOrThrow(long duration, TimeUnit timeUnit) {
        }

        @Override
        public KillAttempt kill() {
            return this;
        }
    }

    /**
     * Gets an immutable instance that represents an attempt where the process is already terminated.
     * This attempt's {@link DestroyAttempt#result() result()} method will return
     * {@link DestroyResult#TERMINATED TERMINATED}.
     * @param <A> kill or term attempt type
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <A extends DestroyAttempt.KillAttempt & DestroyAttempt.TermAttempt> A terminated() {
        return (A) ALREADY_TERMINATED;
    }

    private static final AlreadyTerminated ALREADY_TERMINATED = new AlreadyTerminated();

    private DestroyAttempts() {}

    static class TermAttemptImpl extends AbstractDestroyAttempt implements DestroyAttempt.TermAttempt {

        @SuppressWarnings("unused")
        private static final Logger log = LoggerFactory.getLogger(TermAttemptImpl.class);

        private final BasicProcessDestructor destructor;

        public TermAttemptImpl(BasicProcessDestructor destructor, Process process, DestroyResult result) {
            super(result, process);
            this.destructor = requireNonNull(destructor);
        }

        @Override
        public TermAttempt await() {
            if (result == DestroyResult.TERMINATED) {
                return this;
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                log.debug("interrupted while waiting on process termination: " + e);
            }
            DestroyResult result = destructor.trackCurrentResult();
            return new TermAttemptImpl(destructor, process, result);
        }

        @Override
        public TermAttempt timeout(long duration, TimeUnit unit) {
            if (result == DestroyResult.TERMINATED) {
                return this;
            }
            try {
                boolean finished = process.waitFor(duration, unit);
                if (finished) {
                    return DestroyAttempts.terminated();
                }
            } catch (InterruptedException e) {
                log.info("interrupted: " + e);
            }
            return this;
        }

        @Override
        public KillAttempt kill() {
            if (result == DestroyResult.TERMINATED) {
                return terminated();
            }
            return destructor.sendKillSignal();
        }

    }

    static class KillAttemptImpl extends AbstractDestroyAttempt implements DestroyAttempt.KillAttempt {

        @SuppressWarnings("unused")
        private static final Logger log = LoggerFactory.getLogger(KillAttemptImpl.class);

        public KillAttemptImpl(DestroyResult result, Process process) {
            super(result, process);
        }

        @Override
        public void timeoutOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException {
            boolean succeeded = timeoutKill(duration, timeUnit);
            if (!succeeded) {
                throw new ProcessStillAliveException("kill failed");
            }
        }

        @Override
        public void awaitKill() throws InterruptedException {
            process.waitFor();
        }

        @Override
        public boolean timeoutKill(long duration, TimeUnit unit) {
            try {
                return process.waitFor(duration, unit);
            } catch (InterruptedException e) {
                log.info("interrupted: " + e);
                return false;
            }
        }
    }

}
