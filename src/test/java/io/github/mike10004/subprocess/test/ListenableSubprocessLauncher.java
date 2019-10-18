package io.github.mike10004.subprocess.test;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.github.mike10004.subprocess.BasicSubprocessLauncher;
import io.github.mike10004.subprocess.ProcessTracker;

import java.util.function.Supplier;

public class ListenableSubprocessLauncher extends BasicSubprocessLauncher {

    public ListenableSubprocessLauncher(ProcessTracker processTracker, Supplier<ListeningExecutorService> launchExecutorServiceFactory) {
        super(processTracker, launchExecutorServiceFactory);
    }

}
