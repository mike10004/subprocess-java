package io.github.mike10004.subprocess;

import java.util.concurrent.Future;

interface ProcessExecution<SO, SE> {

    Process getProcess();

    Future<ProcessResult<SO, SE>> getFuture();

}
