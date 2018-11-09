package io.github.mike10004.subprocess;

import com.google.common.util.concurrent.FutureCallback;

import javax.annotation.Nullable;

abstract class AlwaysCallback<T> implements FutureCallback<T> {

    @Override
    public void onSuccess(T result) {
        always(result, null);
    }

    @Override
    public void onFailure(Throwable t) {
        always(null, t);
    }

    protected abstract void always(@Nullable T result, @Nullable Throwable t);
}
