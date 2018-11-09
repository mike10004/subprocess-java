package com.github.mike10004.nativehelper.subprocess;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public abstract class StreamPipe<F0 extends Closeable, T0 extends Closeable> {

    private transient ComponentPair<F0, T0> componentPair;

    private transient final CountDownLatch latch = new CountDownLatch(1);

    public static class ComponentPair<F, T> {
        public final F from;
        public final T to;

        public ComponentPair(F from, T to) {
            this.from = requireNonNull(from);
            this.to = requireNonNull(to);
        }

        public static <F, T> ComponentPair<F, T> of(F from, T to) {
            return new ComponentPair<>(from, to);
        }
    }

    protected abstract ComponentPair<F0, T0> createComponents() throws IOException;

    protected T0 openTo() throws IOException {
        componentPair = createComponents();
        latch.countDown();
        return componentPair.to;
    }

    @SuppressWarnings("RedundantThrows")
    public F0 connect() throws IOException, InterruptedException {
        latch.await();
        checkState(componentPair != null, "BUG: components not yet created");
        return componentPair.from;
    }

    @SuppressWarnings("RedundantThrows")
    public F0 connect(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException {
        boolean succeeded = latch.await(timeout, timeUnit);
        if (!succeeded) {
            throw new IOException("waited for pipe to connect " + timeout + " " + timeUnit + " but it did not; this means that the stream was never opened");
        }
        checkState(componentPair != null, "BUG: components not yet created");
        return componentPair.from;
    }
}
