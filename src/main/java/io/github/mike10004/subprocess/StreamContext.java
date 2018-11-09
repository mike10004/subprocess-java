package io.github.mike10004.subprocess;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface that defines methods for manipulating process output.
 * @param <SO> type of captured standard output content
 * @param <SE> type of captured standard error content
 */
public interface StreamContext<C extends StreamControl, SO, SE> {

    /**
     * Produces the byte sources and byte sinks to be attached to the process
     * standard output, error, and input streams.
     * @return an output context
     * @throws IOException if something goes awry
     */
    C produceControl() throws IOException;

    /**
     * Gets the transform that produces a result from a process exit code.
     * Instances of this class create the process stream sources and sinks
     * (in {@link #produceControl()} and must be able to produce a
     * {@link StreamContent} instance after the process has finished.
     * @param exitCode the process exit code
     * @param context the output context produced by {@link #produceControl()}
     * @return the transform
     */
    StreamContent<SO, SE> transform(int exitCode, C context);

    /**
     * Maps the types of this output control to other types using a pair of functions.
     * @param stdoutMap the standard output map function
     * @param stderrMap the standard error map function
     * @param <SO2> the destination type for standard output content
     * @param <SE2> the destination type for standard error content
     * @return an output control satisfying the requirements of the destination types
     */
    default <SO2, SE2> StreamContext<C, SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        StreamContext<C, SO, SE> self = this;
        return new StreamContext<C, SO2, SE2>() {
            @Override
            public C produceControl() throws IOException {
                return self.produceControl();
            }

            @Override
            public StreamContent<SO2, SE2> transform(int exitCode, C context) {
                return self.transform(exitCode, context).map(stdoutMap, stderrMap);
            }
        };
    }

    /**
     * Interface that represents a stream context whose captured standard output and error
     * content has the same type.
     * @param <S> type of the captured standard output and standard error contents
     * @param <C> type of the stream control
     */
    interface UniformStreamContext<C extends StreamControl, S> extends StreamContext<C, S, S> {

        /**
         * Wraps a process output control whose standard error and output type
         * in an object that satisfies this interface.
         * @param homogenous the uniform stream context
         * @param <C> stream control type
         * @param <S> type of captured standard output and standard error content
         * @return a new uniform stream context
         */
        static <C extends StreamControl, S> UniformStreamContext<C, S> wrap(StreamContext<C, S, S> homogenous) {
            return new UniformStreamContext<C, S>() {
                @Override
                public C produceControl() throws IOException {
                    return homogenous.produceControl();
                }

                @Override
                public StreamContent<S, S> transform(int exitCode, C context) {
                    return homogenous.transform(exitCode, context);
                }
            };
        }

        /**
         * Maps this stream context to one whose output is a different type.
         * @param stmap the map function
         * @param <T> the different type
         * @return the mapped context
         * @see StreamContext#map(Function, Function)
         */
        default <T> UniformStreamContext<C, T> map(Function<? super S, T> stmap) {
            StreamContext<C, S, S> duplex = this;
            return new UniformStreamContext<C, T>() {

                @Override
                public C produceControl() throws IOException {
                    return duplex.produceControl();
                }

                @Override
                public StreamContent<T, T> transform(int exitCode, C context) {
                    return duplex.transform(exitCode, context).map(stmap, stmap);
                }
            };
        }
    }

    /**
     * Creates a new stream context that ignores process output.
     * @param streamControl  the stream control to use
     * @param <C> stream control type
     * @param <SO> type of captured standard output content
     * @param <SE> type of captured standard error content
     * @return the new context
     */
    static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefinedAndOutputIgnored(C streamControl) {
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
    static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefined(C streamControl, Supplier<? extends SO> stdoutProvider, Supplier<? extends SE> stderrProvider) {
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
    static <C extends StreamControl, SO, SE> StreamContext<C, SO, SE> predefined(C streamControl, Supplier<? extends StreamContent<SO, SE>> outputter) {
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

}
