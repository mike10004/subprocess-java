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

}
