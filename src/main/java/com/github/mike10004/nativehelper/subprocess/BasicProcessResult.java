package com.github.mike10004.nativehelper.subprocess;

import static java.util.Objects.requireNonNull;

class BasicProcessResult<SO, SE> implements ProcessResult<SO, SE> {

    private final int exitCode;
    private final StreamContent<SO, SE> output;

    public BasicProcessResult(int exitCode, StreamContent<SO, SE> output) {
        this.exitCode = exitCode;
        this.output = requireNonNull(output);
    }

    @Override
    public int exitCode() {
        return exitCode;
    }

    @Override
    public StreamContent<SO, SE> content() {
        return output;
    }

    public static <SO, SE> BasicProcessResult<SO, SE> withNoOutput(int exitCode) {
        return new BasicProcessResult<>(exitCode, StreamContents.bothNull());
    }

    public static <SO, SE> BasicProcessResult<SO, SE> create(int exitCode, SO stdout, SE stderr) {
        return new BasicProcessResult<>(exitCode, StreamContent.direct(stdout, stderr));
    }

    @Override
    public String toString() {
        return "ProcessResult{" +
                "exitCode=" + exitCode +
                ", output=" + output +
                '}';
    }
}
