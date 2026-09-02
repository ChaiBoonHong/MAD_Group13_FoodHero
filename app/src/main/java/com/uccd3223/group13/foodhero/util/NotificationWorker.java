package com.uccd3223.group13.foodhero.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import java.util.List;

public class NotificationWorker extends Worker {
    public static final String CHANNEL_STUDENT = "foodhero_student_channel";
    public static final String CHANNEL_MERCHANT = "foodhero_merchant_channel";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SessionManager session = SessionManager.getInstance(context);

        if (!session.isLoggedIn()) {
            return Result.success();
        }

        UserRole role = session.getUserRole();
        createNotificationChannels(context);

        FoodHeroRepository.getInstance(context).getNotifications(role, new ResultCallback<List<FoodHeroNotification>>() {
            @Override
            public void onSuccess(List<FoodHeroNotification> list) {
                if (list != null && !list.isEmpty()) {
                    for (FoodHeroNotification n : list) {
                        if (!n.isRead()) {
                            showNotification(context, n, role);
                            break; // Show most recent unread
                        }
                    }
                }
            }

            @Override
            public void onError(DataError error) {
            }
        });

        return Result.success();
    }

    private void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel studentChannel = new NotificationChannel(
                    CHANNEL_STUDENT,
                    "Student FoodHero Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                studentChannel.setDescription("Notifications about nearby surplus bags and orders");
                nm.createNotificationChannel(studentChannel);

                NotificationChannel merchantChannel = new NotificationChannel(
                    CHANNEL_MERCHANT,
                    "Merchant FoodHero Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                merchantChannel.setDescription("Notifications about reservations and stock alerts");
                nm.createNotificationChannel(merchantChannel);
            }
        }
    }

    private void showNotification(Context context, FoodHeroNotification n, UserRole role) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String channelId = (role == UserRole.MERCHANT) ? CHANNEL_MERCHANT : CHANNEL_STUDENT;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_foodhero_logo)
            .setContentTitle(n.getTitle() != null ? n.getTitle() : "FoodHero Alert")
            .setContentText(n.getMessage() != null ? n.getMessage() : "")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
