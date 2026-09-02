package com.uccd3223.group13.foodhero.data.callback;

public interface ResultCallback<T> {
    void onSuccess(T result);
    void onError(DataError error);
}
