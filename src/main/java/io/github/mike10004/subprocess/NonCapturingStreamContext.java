package io.github.mike10004.subprocess;

/**
 * Interface of a stream context that does not capture process output in the result.
 * @param <C> stream control type
 */
public interface NonCapturingStreamContext<C extends StreamControl> extends UniformStreamContext<C, Void> {

    @Override
    default StreamContent<Void, Void> transform(int exitCode, C context) {
        return StreamContent.absent();
    }

}
