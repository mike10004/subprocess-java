package io.github.mike10004.subprocess;

import java.io.IOException;
import java.util.function.Function;

/**
 * Interface that represents a stream context whose captured standard output and error
 * content has the same type.
 * @param <S> type of the captured standard output and standard error contents
 * @param <C> type of the stream control
 */
public interface UniformStreamContext<C extends StreamControl, S> extends StreamContext<C, S, S> {

    /**
     * Wraps a process output control whose standard error and output type
     * in an object that satisfies this interface.
     * @param homogenous the uniform stream context
     * @param <C> stream control type
     * @param <S> type of captured standard output and standard error content
     * @return a new uniform stream context
     */
    static <C extends StreamControl, S> io.github.mike10004.subprocess.UniformStreamContext<C, S> wrap(StreamContext<C, S, S> homogenous) {
        return new io.github.mike10004.subprocess.UniformStreamContext<C, S>() {
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
    default <T> io.github.mike10004.subprocess.UniformStreamContext<C, T> map(Function<? super S, T> stmap) {
        StreamContext<C, S, S> duplex = this;
        return new io.github.mike10004.subprocess.UniformStreamContext<C, T>() {

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
