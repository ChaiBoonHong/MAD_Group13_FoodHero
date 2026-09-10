package com.uccd3223.group13.foodhero.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;

public class OrderExpirationWorker extends Worker {
    public static final String KEY_ORDER_ID = "order_id";

    public OrderExpirationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String orderId = getInputData().getString(KEY_ORDER_ID);
        if (orderId != null && !orderId.trim().isEmpty()) {
            FoodHeroRepository repo = FoodHeroRepository.getInstance(getApplicationContext());
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            repo.expireUnpaidOrder(orderId, new com.uccd3223.group13.foodhero.data.callback.ResultCallback<com.uccd3223.group13.foodhero.data.model.Order>() {
                @Override
                public void onSuccess(com.uccd3223.group13.foodhero.data.model.Order result) {
                    latch.countDown();
                }

                @Override
                public void onError(com.uccd3223.group13.foodhero.data.callback.DataError error) {
                    latch.countDown();
                }
            });
            try {
                latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
        return Result.success();
    }
}
