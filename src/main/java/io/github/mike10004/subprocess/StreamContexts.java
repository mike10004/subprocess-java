package io.github.mike10004.subprocess;

import io.github.mike10004.subprocess.StreamContext.UniformStreamContext;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class StreamContexts {

    private static final StreamContent NO_OUTPUT = StreamContent.direct(null, null);

    @SuppressWarnings("unchecked")
    static <SO, SE> StreamContent<SO, SE> noOutput() {
        return NO_OUTPUT;
    }

    public static StreamContext<? extends StreamControl, Void, Void> sinkhole() {
        return predefined(PredefinedStreamControl.nullWithNullInput(), StreamContexts::noOutput);
    }

    public static UniformStreamContext<? extends StreamControl, StreamInput> memoryByteSources(@Nullable StreamInput stdin) {
        return byteArrays(stdin).map(StreamInput::wrap);
    }

    public static UniformStreamContext<? extends StreamControl, File> outputTempFiles(Path directory, @Nullable StreamInput stdin) {
        return new FileStreamContext() {
            @Override
            public FileStreamControl produceControl() throws IOException {
                File stdoutFile = File.createTempFile("FileStreamContext_stdout", ".tmp", directory.toFile());
                File stderrFile = File.createTempFile("FileStreamContext_stderr", ".tmp", directory.toFile());
                return new FileStreamControl(stdoutFile, stderrFile, stdin);
            }
        };
    }

    public static UniformStreamContext<? extends StreamControl, File> outputFiles(File stdoutFile, File stderrFile, @Nullable StreamInput stdin) {
        return new FileStreamContext() {
            @Override
            public FileStreamControl produceControl() {
                return new FileStreamControl(stdoutFile, stderrFile, stdin);
            }
        };
    }

    /**
     * Creates a new stream context that ignores process output.
     * @param streamControl  the stream control to use
     * @param <C> stream control type
     * @param <SO> type of captured standard output content
     * @param <SE> type of captured standard error content
     * @return the new context
     */
    public static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefinedAndOutputIgnored(C streamControl) {
        return predefined(streamControl, StreamContexts::noOutput);
    }

    /**
     * Creates a new stream context whose output content is supplied by the given suppliers.
     * @param streamControl the stream control
     * @param stdoutProvider the standard output content supplier
     * @param stderrProvider the standard error content supplier
     * @param <C> stream control type
     * @param <SO> type of captured standard output content
     * @param <SE> type of captured standard error content
     * @return the new context
     */
    public static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefined(C streamControl, Supplier<? extends SO> stdoutProvider, Supplier<? extends SE> stderrProvider) {
        return predefined(streamControl, () -> StreamContent.direct(stdoutProvider.get(), stderrProvider.get()));
    }

    /**
     * Creates a new stream context whose output is supplied by the given supplier.
     * @param streamControl the stream control
     * @param outputter the standard output and error content supplier
     * @param <C> stream control type
     * @param <SO> type of captured standard output content
     * @param <SE> type of captured standard error content
     * @return the new context
     */
    public static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefined(C streamControl, Supplier<? extends StreamContent<SO, SE>> outputter) {
        return new StreamContext<C, SO, SE>() {
            @Override
            public C produceControl() {
                return streamControl;
            }

            @Override
            public StreamContent<SO, SE> transform(int exitCode, C context) {
                return outputter.get();
            }

        };
    }

    @VisibleForTesting
    static class BucketContext extends PredefinedStreamControl {

        public final ByteBucket stdout;
        public final ByteBucket stderr;

        public BucketContext(@Nullable StreamInput stdin) {
            this(ByteBucket.withInitialCapacity(256), ByteBucket.withInitialCapacity(256), stdin);
        }

        public BucketContext(ByteBucket stdout, ByteBucket stderr, @Nullable StreamInput stdin) {
            super(stdout, stderr, stdin);
            this.stdout = requireNonNull(stdout);
            this.stderr = requireNonNull(stderr);
        }


    }

    public static UniformStreamContext<? extends StreamControl, byte[]> byteArrays(@Nullable StreamInput stdin) {
        return new UniformStreamContext<BucketContext, byte[]>() {
            @Override
            public BucketContext produceControl() {
                return new BucketContext(stdin);
            }

            @Override
            public StreamContent<byte[], byte[]> transform(int exitCode, BucketContext ctx) {
                return StreamContent.direct(ctx.stdout.dump(), ctx.stderr.dump());
            }
        };
    }

    public static UniformStreamContext<? extends StreamControl, String> strings(Charset charset, @Nullable StreamInput stdin) {
        requireNonNull(charset);
        return byteArrays(stdin).map(bytes -> new String(bytes, charset));
    }

    public static StreamContext<? extends StreamControl, Void, Void> inheritOutputs() {
        return predefined(PredefinedStreamControl.builder().inheritStderr().inheritStdout().build(), StreamContexts::noOutput);    }

    public static StreamContext<? extends StreamControl, Void, Void> inheritAll() {
        return predefined(PredefinedStreamControl.builder().inheritStdin().inheritStderr().inheritStdout().build(), StreamContexts::noOutput);
    }

    public static class FileStreamControl implements StreamControl {
        private final File stdoutFile, stderrFile;
        @Nullable
        private final StreamInput stdin;

        public FileStreamControl(File stdoutFile, File stderrFile, @Nullable StreamInput stdin) {
            this.stdoutFile = requireNonNull(stdoutFile);
            this.stderrFile = requireNonNull(stderrFile);
            this.stdin = stdin;
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return new FileOutputStream(stdoutFile);
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return new FileOutputStream(stderrFile);
        }

        @Nullable
        @Override
        public InputStream openStdinSource() throws IOException {
            return stdin == null ? null : stdin.openStream();
        }
    }

    public static abstract class FileStreamContext implements UniformStreamContext<FileStreamControl, File> {

        @Override
        public StreamContent<File, File> transform(int exitCode, FileStreamControl context) {
            return StreamContent.direct(context.stdoutFile, context.stderrFile);
        }

    }

}
