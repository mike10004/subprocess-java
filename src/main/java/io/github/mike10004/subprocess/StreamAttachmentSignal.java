package io.github.mike10004.subprocess;

import java.util.concurrent.TimeUnit;

interface StreamAttachmentSignal {

    /**
     * Notifies this signal that the streams have been attached.
     */
    void notifyStreamsAttached();

    /**
     * Blocks on the current thread until {@link #notifyStreamsAttached()} is invoked.
     * @param duration timeout
     * @param unit timeout unit
     * @return true if {@code notifyStreamsAttached()} is invoked before timeout elapses, false otherwise
     * @throws InterruptedException if interrupted while waiting
     */
    boolean await(long duration, TimeUnit unit) throws InterruptedException;

}
