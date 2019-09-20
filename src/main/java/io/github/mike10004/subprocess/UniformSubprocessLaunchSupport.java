package io.github.mike10004.subprocess;

import java.util.function.Function;

/**
 * Class that represents launch support with a uniform stream context.
 * @param <S> type of captured standard output and standard error content
 */
public final class UniformSubprocessLaunchSupport<S> extends SubprocessLaunchSupport<S, S> {

    UniformSubprocessLaunchSupport(Subprocess subprocess, SubprocessLauncher launcher, StreamContext<?, S, S> streamContext) {
        super(subprocess, launcher, streamContext);
    }

    /**
     * Returns a new launcher that maps captured standard output and standard error
     * content to a different type.
     * @param mapper map function
     * @param <T> destination type
     * @return a new launcher instance
     */
    public <T> UniformSubprocessLaunchSupport<T> map(Function<? super S, T> mapper) {
        StreamContext.UniformStreamContext<?, S> u = StreamContext.UniformStreamContext.wrap(this.streamContext);
        StreamContext.UniformStreamContext<?, T> t = u.map(mapper);
        return uniformOutput(t);
    }

}
