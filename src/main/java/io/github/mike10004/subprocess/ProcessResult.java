package io.github.mike10004.subprocess;

import java.util.function.Function;

/**
 * Interface representing a process result. A process result is an exit code
 * and an object representing output from the process.
 * @param <SO> type of the captured standard output contents
 * @param <SE> type of the captured standard error contents
 */
public interface ProcessResult<SO, SE> {

    int exitCode();

    StreamContent<SO, SE> content();

    static <SO, SE> ProcessResult<SO, SE> direct(int exitCode, SO stdout, SE stderr) {
        return BasicProcessResult.create(exitCode, stdout, stderr);
    }

    static <SO, SE> ProcessResult<SO, SE> direct(int exitCode, StreamContent<SO, SE> output) {
        return new BasicProcessResult<>(exitCode, output);
    }

    default <SO2, SE2> ProcessResult<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        return new BasicProcessResult<>(exitCode(), content().map(stdoutMap, stderrMap));
    }
}
