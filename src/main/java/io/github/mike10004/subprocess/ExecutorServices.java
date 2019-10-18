package io.github.mike10004.subprocess;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Supplier;

class ExecutorServices {

    private ExecutorServices() {}

    private static final String VALID_POOL_NAME_REGEX = "^[-\\w]+$";

    public static <T> Supplier<T> newTransformedSingleThreadExecutorServiceFactory(@Nullable String poolName, Function<? super ExecutorService, T> transform) {
        Supplier<ExecutorService> serviceSupplier = newSingleThreadExecutorServiceFactory(poolName);
        return () -> {
            ExecutorService service = serviceSupplier.get();
            return transform.apply(service);
        };
    }

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
