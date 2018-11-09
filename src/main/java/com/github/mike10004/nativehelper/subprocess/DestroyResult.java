package com.github.mike10004.nativehelper.subprocess;

/**
 * Enumeration of constants that represent possible process states after destroying
 * a process has been attempted. To attempt to destroy a process means, in this context,
 * to send SIGTERM or SIGKILL to the process.
 */
public enum DestroyResult {

    /**
     * Constant that indicates the process has stopped, either naturally or because of the signal.
     */
    TERMINATED,

    /**
     * Constant that indicates the process is still alive.
     */
    STILL_ALIVE

}
