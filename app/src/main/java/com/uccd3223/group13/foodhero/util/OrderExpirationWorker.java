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
            repo.checkAndExpireOrder(orderId);
        }
        return Result.success();
    }
}
