/*
 * (most of this code is from Ant's PumpStreamHandler}
 *
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
package com.github.mike10004.nativehelper.subprocess;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A customized version of {@code org.apache.tools.ant.taskdefs.PumpStreamHandler}.
 */
class StreamConduit {

    private volatile Thread outputThread;
    private volatile Thread errorThread;
    private volatile Thread inputThread;

    private final OutputStream out;
    private final OutputStream err;
    @Nullable
    private final InputStream input;

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param out the output <code>OutputStream</code>.
     * @param err the error <code>OutputStream</code>.
     * @param input the input <code>InputStream</code>.
     */
    public StreamConduit(OutputStream out, OutputStream err, @Nullable InputStream input) {
        this.out = out;
        this.err = err;
        this.input = input;
    }

    /**
     * Set the <code>InputStream</code> from which to read the
     * standard output of the process.
     * @param is the <code>InputStream</code>.
     */
    private void setProcessOutputStream(InputStream is) {
        createProcessOutputPump(is, out);
    }

    /**
     * Set the <code>InputStream</code> from which to read the
     * standard error of the process.
     * @param is the <code>InputStream</code>.
     */
    private void setProcessErrorStream(InputStream is) {
        if (err != null) {
            createProcessErrorPump(is, err);
        }
    }

    /**
     * Set the <code>OutputStream</code> by means of which
     * input can be sent to the process.
     * @param os the <code>OutputStream</code>.
     */
    private void setProcessInputStream(OutputStream os) {
        if (input != null) {
            inputThread = createPump(input, os, true);
        } else {
            AntFileUtils.close(os);
        }
    }

    /**
     * Start the <code>Thread</code>s.
     */
    public java.io.Closeable connect(OutputStream stdin, InputStream stdout, InputStream stderr) {
        setProcessInputStream(stdin);
        setProcessErrorStream(stderr);
        setProcessOutputStream(stdout);
        outputThread.start();
        errorThread.start();
        if (inputThread != null) {
            inputThread.start();
        }
        return new Closeable() {
            @Override
            public void close() {
                stop();
            }
        };
    }

    /**
     * Stop pumping the streams.
     */
    private void stop() {
        finish(inputThread);

        try {
            err.flush();
        } catch (IOException e) {
            // ignore
        }
        try {
            out.flush();
        } catch (IOException e) {
            // ignore
        }
        finish(outputThread);
        finish(errorThread);
    }

    private static final long JOIN_TIMEOUT = 200;

    /**
     * Waits for a thread to finish while trying to make it finish
     * quicker by stopping the pumper (if the thread is a {@code
     * PumpStreamHandler.ThreadWithPumper ThreadWithPumper} instance) or interrupting
     * the thread.
     *
     * @since Ant 1.8.0
     */
    private void finish(Thread t) {
        if (t == null) {
            // nothing to terminate
            return;
        }
        try {
            BlockingStreamPumper s = null;
            if (t instanceof ThreadWithPumper) {
                s = ((ThreadWithPumper) t).getPumper();
            }
            if (s != null && s.isFinished()) {
                return;
            }
            if (!t.isAlive()) {
                return;
            }

            if (s != null && !s.isFinished()) {
                s.stop();
            }
            t.join(JOIN_TIMEOUT);
            while ((s == null || !s.isFinished()) && t.isAlive()) {
                t.interrupt();
                t.join(JOIN_TIMEOUT);
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * Get the error stream.
     * @return <code>OutputStream</code>.
     */
    protected OutputStream getErr() {
        return err;
    }

    /**
     * Get the output stream.
     * @return <code>OutputStream</code>.
     */
    protected OutputStream getOut() {
        return out;
    }

    private static final boolean CLOSE_STDOUT_AND_STDERR_INSTREAMS_WHEN_EXHAUSTED = true;

    /**
     * Create the pump to handle process output.
     * @param is the <code>InputStream</code>.
     * @param os the <code>OutputStream</code>.
     */
    private void createProcessOutputPump(InputStream is, OutputStream os) {
        outputThread = createPump(is, os, CLOSE_STDOUT_AND_STDERR_INSTREAMS_WHEN_EXHAUSTED);
    }

    /**
     * Create the pump to handle error output.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     */
    private void createProcessErrorPump(InputStream is, OutputStream os) {
        errorThread = createPump(is, os, CLOSE_STDOUT_AND_STDERR_INSTREAMS_WHEN_EXHAUSTED);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     * @param closeWhenExhausted if true close the inputstream.
     * @return a thread object that does the pumping, subclasses
     * should return an instance of {@code PumpStreamHandler.ThreadWithPumper
     * ThreadWithPumper}.
     */
    private Thread createPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted) {
        BlockingStreamPumper pumper = new BlockingStreamPumper(is, os, closeWhenExhausted);
        // pumper.setAutoflush(true); // always auto-flush
        final Thread result = new ThreadWithPumper(pumper);
        result.setDaemon(true);
        return result;
    }

    /**
     * Specialized subclass that allows access to the running StreamPumper.
     *
     * @since Ant 1.8.0
     */
    protected static class ThreadWithPumper extends Thread {
        private final BlockingStreamPumper pumper;
        public ThreadWithPumper(BlockingStreamPumper p) {
            super(p);
            pumper = p;
        }
        protected BlockingStreamPumper getPumper() {
            return pumper;
        }
    }

}
