{# NOTE: This is a source file from which README.md is generated #}
{# TODO: add Travis/Appveyor/Maven badges #}

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
        <groupId>${project_groupId}</groupId>
        <artifactId>${project_artifactId}</artifactId>
        <version>${project_version}</version>
    <dependency>

and use  

    import io.github.mike10004.subprocess.*;

to import the classes. (Note that the `groupId` is `com.github.mike10004` but 
the package starts with `io.github.mike10004`. I recognize that this is an 
unfortunate inconsistency.)

### Launch process and capture output

${readme_example_launchAndCaptureStrings}

### Launch process and write output to file

${readme_example_launchAndCaptureFiles}

### Feed standard input to process

${readme_example_feedStandardInput}

### Terminate a process

${readme_example_terminate}

### Launch process and tail output

${readme_example_tailOutput}

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
