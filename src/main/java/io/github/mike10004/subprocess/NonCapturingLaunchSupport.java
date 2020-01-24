package io.github.mike10004.subprocess;

/**
 * Launch support implementation for stream contexts that do not capture process output.
 * You probably won't need to instantiate this directly.
 * @see TailingLaunchSupport
 */
public class NonCapturingLaunchSupport extends UniformSubprocessLaunchSupport<Void> {

    public NonCapturingLaunchSupport(Subprocess subprocess, SubprocessLauncher launcher, NonCapturingStreamContext<?> streamContext) {
        super(subprocess, launcher, streamContext);
    }

}
