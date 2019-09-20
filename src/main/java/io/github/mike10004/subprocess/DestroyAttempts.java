package io.github.mike10004.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

class DestroyAttempts {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(DestroyAttempts.class);

    private static final class AlreadyTerminated implements SigkillAttempt, SigtermAttempt {

        @Override
        public DestroyResult result() {
            return DestroyResult.TERMINATED;
        }

        @Override
        public SigtermAttempt await() {
            return this;
        }

        @Override
        public SigtermAttempt await(long duration, TimeUnit unit) {
            return this;
        }

        @Override
        public void awaitKill() {
        }

        @Override
        public SigkillAttempt tryAwaitKill(long duration, TimeUnit timeUnit) {
            return this;
        }

        @Override
        public void awaitOrThrow(long duration, TimeUnit timeUnit) {
        }

        @Override
        public SigkillAttempt kill() {
            return this;
        }

        @Override
        public String toString() {
            return "DestroyAttempt{TERMINATED}";
        }
    }

    /**
     * Gets an immutable instance that represents an attempt where the process is already terminated.
     * This attempt's {@link DestroyAttempt#result() result()} method will return
     * {@link DestroyResult#TERMINATED TERMINATED}.
     * @param <A> kill or term attempt type
     * @return an attempt instance
     */
    @SuppressWarnings("unchecked")
    public static <A extends SigkillAttempt & SigtermAttempt> A terminated() {
        return (A) ALREADY_TERMINATED;
    }

    private static final AlreadyTerminated ALREADY_TERMINATED = new AlreadyTerminated();

    private DestroyAttempts() {}

    static abstract class AbstractDestroyAttempt implements DestroyAttempt {

        protected final DestroyResult result;
        protected final Process process;

        public AbstractDestroyAttempt(DestroyResult result, Process process) {
            this.result = requireNonNull(result);
            this.process = requireNonNull(process);
        }

        /**
         * Gets the result of the attempt.
         * @return the result
         */
        @Override
        public DestroyResult result() {
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s{%s}", getClass().getSimpleName(), result);
        }
    }

    final static class TermAttemptImpl extends AbstractDestroyAttempt implements SigtermAttempt {

        private static final Logger log = LoggerFactory.getLogger(TermAttemptImpl.class);

        private final BasicProcessDestructor destructor;

        public TermAttemptImpl(BasicProcessDestructor destructor, Process process, DestroyResult result) {
            super(result, process);
            this.destructor = requireNonNull(destructor);
        }

        @Override
        public SigtermAttempt await() {
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
        public SigtermAttempt await(long duration, TimeUnit unit) {
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
        public SigkillAttempt kill() {
            if (result == DestroyResult.TERMINATED) {
                return terminated();
            }
            return destructor.sendKillSignal();
        }

    }

    final static class KillAttemptImpl extends AbstractDestroyAttempt implements SigkillAttempt {

        private static final Logger log = LoggerFactory.getLogger(KillAttemptImpl.class);

        public KillAttemptImpl(DestroyResult result, Process process) {
            super(result, process);
        }

        @Override
        public void awaitOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException {
            boolean succeeded = tryAwaitKill(duration, timeUnit).result() == DestroyResult.TERMINATED;
            if (!succeeded) {
                throw new ProcessStillAliveException("kill failed");
            }
        }

        @Override
        public void awaitKill() throws InterruptedException {
            process.waitFor();
        }

        @Override
        public SigkillAttempt tryAwaitKill(long duration, TimeUnit unit) {
            boolean terminated = false;
            try {
                terminated = process.waitFor(duration, unit);
            } catch (InterruptedException e) {
                log.info("interrupted: " + e);
            }
            return terminated ? ALREADY_TERMINATED : this;
        }
    }

}
