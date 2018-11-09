package io.github.mike10004.subprocess;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static io.github.mike10004.subprocess.Preconditions.checkArgument;

class ExecutorServices {

    private ExecutorServices() {}

    private static final String VALID_POOL_NAME_REGEX = "^[-\\w]+$";

    public static Supplier<ExecutorService> newSingleThreadExecutorServiceFactory(@Nullable String poolName) {
        Preconditions.checkArgument(poolName == null || poolName.matches(VALID_POOL_NAME_REGEX), "pool name characters are restricted to %s", VALID_POOL_NAME_REGEX);
        return () -> {
            String prefix = poolName == null ? "subprocess" : poolName;
            ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(prefix + "-%d")
                    .build();
            ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);
            return service;
        };
    }

}
