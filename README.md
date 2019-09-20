


# subprocess-java

Fluent Java library for launching processes by running executable binaries 
outside the JVM.

## Design goals

* use asynchronous patterns for process launch and termination
* support customization but provide sensible defaults 
* cleanly separate of value classes and service classes/interfaces
* avoid dependencies to allow library users to do their own thing
* support Windows and Linux (and MacOS for the most part, but without testing)

## Quick Start

Include the dependency with

    <dependency>
        <groupId>com.github.mike10004</groupId>
        <artifactId>subprocess</artifactId>
        <version>0.1</version>
    <dependency>

and use  

    import io.github.mike10004.subprocess.*;

to import the classes. (Note that the `groupId` is `com.github.mike10004` but 
the package starts with `io.github.mike10004`. I recognize that this is an 
unfortunate inconsistency.)

### Launch process and capture output

    // <String, String> parameters refer to type of captured stdout and stderr data
    ProcessResult<String, String> result;
    try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
        result = Subprocess.running("echo")
                .arg("hello, world")
                .build()
                .launcher(processTracker)
                .outputStrings(Charset.defaultCharset())
                .launch()
                .await();
    }
    System.out.println(result.content().stdout()); // prints "hello, world"


### Launch process and write output to file

    ProcessResult<File, File> result;
    try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
        ProcessMonitor<File, File> monitor = Subprocess.running("echo")
                .arg("0123456789")
                .build()
                .launcher(processTracker)
                .outputTempFiles(new File(System.getProperty("java.io.tmpdir")).toPath())
                .launch();
        result = monitor.await();
    }
    File stdoutFile = result.content().stdout();
    System.out.format("%d bytes written to %s%n", stdoutFile.length(), stdoutFile);
    stdoutFile.delete();
    result.content().stderr().delete();


### Feed standard input to process

    StreamInput input = StreamInput.fromFile(new File("/etc/passwd"));
    ProcessResult<String, String> result;
    try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
        ProcessMonitor<String, String> monitor = Subprocess.running("grep")
                .arg("root")
                .build()
                .launcher(processTracker)
                .outputStrings(Charset.defaultCharset(), input)
                .launch();
        result = monitor.await();
    }
    System.out.println("grepped " + result.content().stdout());  // prints 'root' line from /etc/passwd


### Terminate a process

    try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
        ProcessMonitor<String, String> monitor = Subprocess.running("cat")
                .arg("-")
                .build()
                .launcher(processTracker)
                .outputStrings(Charset.defaultCharset())
                .launch();
        System.out.println("process alive? " + monitor.process().isAlive());
        SigtermAttempt attempt = monitor.destructor().sendTermSignal()
                .await(3, TimeUnit.SECONDS);
        System.out.println("process alive? " + monitor.process().isAlive());
        if (monitor.process().isAlive()) {
            attempt.kill().awaitOrThrow(3, TimeUnit.SECONDS);
        }
    }


### Launch process and tail output

    PipedInputStream pipeInput = new PipedInputStream();
    StreamControl ctrl = new StreamControl() {
        @Override
        public OutputStream openStdoutSink() throws IOException {
            return new PipedOutputStream(pipeInput);
        }
        @Override
        public OutputStream openStderrSink() {
            return System.err; // in real life, wrap this with CloseShieldOutputStream to avoid closing JVM stderr
        }
        @Nullable
        @Override
        public InputStream openStdinSource() {
            return null;
        }
    };
    StreamContext<StreamControl, Void, Void> ctx = new StreamContext<StreamControl, Void, Void>() {
        @Override
        public StreamControl produceControl() throws IOException {
            return ctrl;
        }
        @Override
        public StreamContent<Void, Void> transform(int exitCode, StreamControl context) {
            return StreamContent.absent();
        }
    };
    ProcessResult<Void, Void> result;
    try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
        // launch a process that prints a number every second
        ProcessMonitor<Void, Void> monitor = Subprocess.running("bash")
                .arg("-c")
                .arg("set -e; for N in $(seq 5) ; do sleep 1 ; echo $N ; done")
                .build()
                .launcher(processTracker)
                .output(ctx)
                .launch();
        // connect a reader to the piped input stream and echo the process output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pipeInput, US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("line: " + line);
            }
        }
        result = monitor.await();
    }
    System.out.println("exit code: " + result.exitCode());


## Motivations

The other libraries I've used for process manipulation either do not offer 
fine enough control over process execution or require too much boilerplate,
duplicative code to exercise fine control. For example, Apache Ant offers a 
robust execution framework but one that doesn't support process termination. 
The base Java `ProcessBuilder` API provides control over everything, but it 
requires a lot of code to make it work how you want.

Furthermore, I wanted an API that reflects the asynchronous nature of process 
execution and termination. Execution is asynchronous by definition, as we're 
launching a new thread in a separate process. Termination is also asynchronous 
because you're sending a signal to a process and not getting a direct response.
