/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.github.mike10004.subprocess;

import java.io.InputStream;
import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Copies all data from an input stream to an output stream.
 *
 * @since Ant 1.2
 */
class BlockingStreamPumper implements Runnable {

    private static final int SMALL_BUFFER_SIZE = 128;

    private final InputStream is;
    private final OutputStream os;
    private volatile boolean finish;
    private volatile boolean finished;
    private final boolean closeWhenExhausted;
    private final static boolean autoflush = true;
    private Exception exception = null;
    private int bufferSize = SMALL_BUFFER_SIZE;
    private boolean started = false;

    /**
     * Create a new StreamPumper.
     *
     * @param is input stream to read data from
     * @param os output stream to write data to.
     * @param closeWhenExhausted if true, the output stream will be closed when
     *        the input is exhausted.
     */
    public BlockingStreamPumper(InputStream is, OutputStream os, boolean closeWhenExhausted) {
        this.is = requireNonNull(is);
        this.os = requireNonNull(os);
        this.closeWhenExhausted = closeWhenExhausted;
    }

    /**
     * Copies data from the input stream to the output stream.
     *
     * Terminates as soon as the input stream is closed or an error occurs.
     */
    public void run() {
        synchronized (this) {
            started = true;
        }
        finished = false;

        final byte[] buf = new byte[bufferSize];

        int length;
        try {
            while (true) {
                if (finish || Thread.interrupted()) {
                    break;
                }

                length = is.read(buf);
                if (length <= 0 || Thread.interrupted()) {
                    break;
                }
                os.write(buf, 0, length);
                if (autoflush) {
                    os.flush();
                }
                if (finish) { //NOSONAR
                    break;
                }
            }
            // On completion, drain any available data (which might be the first data available for quick executions)
            if (finish) {
                while((length = is.available()) > 0) {
                    if (Thread.interrupted()) {
                        break;
                    }
                    length = is.read(buf, 0, Math.min(length, buf.length));
                    if (length <= 0) {
                        break;
                    }
                    os.write(buf, 0, length);
                }
            }
            os.flush();
        } catch (Exception e) {
            synchronized (this) {
                exception = e;
            }
        } finally {
            if (closeWhenExhausted) {
                Streams.close(os);
            }
            finished = true;
            finish = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Tells whether the end of the stream has been reached.
     * @return true is the stream has been exhausted.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * This method blocks until the StreamPumper finishes.
     * @throws InterruptedException if interrupted.
     * @see #isFinished()
     */
    @SuppressWarnings("unused")
    public synchronized void waitFor() throws InterruptedException {
        while (!isFinished()) {
            wait();
        }
    }

    /**
     * Set the size in bytes of the read buffer.
     * @param bufferSize the buffer size to use.
     * @throws IllegalStateException if the StreamPumper is already running.
     */
    @SuppressWarnings("unused")
    public synchronized void setBufferSize(int bufferSize) {
        if (started) {
            throw new IllegalStateException("Cannot set buffer size on a running StreamPumper");
        }
        this.bufferSize = bufferSize;
    }

    /**
     * Get the size in bytes of the read buffer.
     * @return the int size of the read buffer.
     */
    @SuppressWarnings("unused")
    public synchronized int getBufferSize() {
        return bufferSize;
    }

    /**
     * Get the exception encountered, if any.
     * @return the Exception encountered.
     */
    public synchronized Exception getException() {
        return exception;
    }

    /**
     * Stop the pumper as soon as possible.
     * Note that it may continue to block on the input stream
     * but it will really stop the thread as soon as it gets EOF
     * or any byte, and it will be marked as finished.
     * @since Ant 1.6.3
     */
    /*package*/ synchronized void stop() {
        finish = true;
        notifyAll();
    }

}
