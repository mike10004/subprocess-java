/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.mike10004.subprocess;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static java.util.Objects.requireNonNull;

class Streams {

    private Streams() {}

    public static OutputStream nullOutputStream() {
        return NULL_OUTPUT_STREAM;
    }

    /*
     * Copyright (C) 2007 The Guava Authors
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
     * in compliance with the License. You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under the License
     * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
     * or implied. See the License for the specific language governing permissions and limitations under
     * the License.
     */
    @SuppressWarnings("NullableProblems")
    private static final OutputStream NULL_OUTPUT_STREAM =
            new OutputStream() {
                /** Discards the specified byte. */
                @Override
                public void write(int b) {}

                /** Discards the specified byte array. */
                @Override
                public void write(byte[] b) {
                    requireNonNull(b);
                }

                /** Discards the specified byte array. */
                @Override
                public void write(byte[] b, int off, int len) {
                    requireNonNull(b);
                }

                @Override
                public String toString() {
                    return "ByteStreams.nullOutputStream()";
                }
            };

    /** Creates a new byte array for buffering reads or writes. */
    static byte[] createBuffer() {
        return new byte[8192];
    }

    /**
     * There are three methods to implement {@link FileChannel#transferTo(long, long,
     * WritableByteChannel)}:
     *
     * <ol>
     *   <li>Use sendfile(2) or equivalent. Requires that both the input channel and the output
     *       channel have their own file descriptors. Generally this only happens when both channels
     *       are files or sockets. This performs zero copies - the bytes never enter userspace.
     *   <li>Use mmap(2) or equivalent. Requires that either the input channel or the output channel
     *       have file descriptors. Bytes are copied from the file into a kernel buffer, then directly
     *       into the other buffer (userspace). Note that if the file is very large, a naive
     *       implementation will effectively put the whole file in memory. On many systems with paging
     *       and virtual memory, this is not a problem - because it is mapped read-only, the kernel
     *       can always page it to disk "for free". However, on systems where killing processes
     *       happens all the time in normal conditions (i.e., android) the OS must make a tradeoff
     *       between paging memory and killing other processes - so allocating a gigantic buffer and
     *       then sequentially accessing it could result in other processes dying. This is solvable
     *       via madvise(2), but that obviously doesn't exist in java.
     *   <li>Ordinary copy. Kernel copies bytes into a kernel buffer, from a kernel buffer into a
     *       userspace buffer (byte[] or ByteBuffer), then copies them from that buffer into the
     *       destination channel.
     * </ol>
     *
     * This value is intended to be large enough to make the overhead of system calls negligible,
     * without being so large that it causes problems for systems with atypical memory management if
     * approaches 2 or 3 are used.
     */
    private static final int ZERO_COPY_CHUNK_SIZE = 512 * 1024;

    /**
     * Copies all bytes from the input stream to the output stream. Does not close or flush either
     * stream.
     *
     * @param from the input stream to read from
     * @param to the output stream to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    //@CanIgnoreReturnValue
    public static long copy(InputStream from, OutputStream to) throws IOException {
        requireNonNull(from);
        requireNonNull(to);
        byte[] buf = createBuffer();
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    /**
     * Copies all bytes from the readable channel to the writable channel. Does not close or flush
     * either channel.
     *
     * @param from the readable channel to read from
     * @param to the writable channel to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    //@CanIgnoreReturnValue
    public static long copy(ReadableByteChannel from, WritableByteChannel to) throws IOException {
        requireNonNull(from);
        requireNonNull(to);
        if (from instanceof FileChannel) {
            FileChannel sourceChannel = (FileChannel) from;
            long oldPosition = sourceChannel.position();
            long position = oldPosition;
            long copied;
            do {
                copied = sourceChannel.transferTo(position, ZERO_COPY_CHUNK_SIZE, to);
                position += copied;
                sourceChannel.position(position);
            } while (copied > 0 || position < sourceChannel.size());
            return position - oldPosition;
        }

        ByteBuffer buf = ByteBuffer.wrap(createBuffer());
        long total = 0;
        while (from.read(buf) != -1) {
            buf.flip();
            while (buf.hasRemaining()) {
                total += to.write(buf);
            }
            buf.clear();
        }
        return total;
    }

    /**
     * Reads all bytes from an input stream into a byte array. Does not close the stream.
     *
     * @param in the input stream to read from
     * @return a byte array containing all the bytes from the stream
     * @throws IOException if an I/O error occurs
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        // Presize the ByteArrayOutputStream since we know how large it will need
        // to be, unless that value is less than the default ByteArrayOutputStream
        // size (32).
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, in.available()));
        copy(in, out);
        return out.toByteArray();
    }

}
