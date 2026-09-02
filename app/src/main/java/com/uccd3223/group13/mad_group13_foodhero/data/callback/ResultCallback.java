package com.uccd3223.group13.mad_group13_foodhero.data.callback;

public interface ResultCallback<T> {
    void onSuccess(T result);
    void onError(DataError error);
}
