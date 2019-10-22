package io.github.mike10004.subprocess;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class StreamAttachmentLatch implements StreamAttachmentSignal {

    private final CountDownLatch countDownLatch;

    public StreamAttachmentLatch() {
        countDownLatch = new CountDownLatch(1);
    }

    @Override
    public void notifyStreamsAttached() {
        countDownLatch.countDown();
    }

    @Override
    public boolean await(long duration, TimeUnit unit) throws InterruptedException {
        return countDownLatch.await(duration, unit);
    }
}
