package com.uccd3223.group13.foodhero.data.remote;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Real-time notification client communicating directly over Supabase Realtime WebSocket.
 * Operates without Firebase Cloud Messaging (FCM).
 * Handles Phoenix v2 postgres_changes subscription, periodic heartbeat keep-alive,
 * in-app listener dispatch, and native Android system heads-up notifications.
 */
public class SupabaseRealtimeClient {
    private static final String TAG = "SupabaseRealtime";
    public static final String REALTIME_CHANNEL_ID = "foodhero_supabase_realtime";

    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    private WebSocket webSocket;
    private boolean isSubscribed = false;
    private NotificationListener listener;
    private String currentUserId;
    private long heartbeatRef = 1;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (webSocket != null && isSubscribed) {
                try {
                    JsonObject hb = new JsonObject();
                    hb.addProperty("topic", "phoenix");
                    hb.addProperty("event", "heartbeat");
                    hb.add("payload", new JsonObject());
                    hb.addProperty("ref", "hb_" + (heartbeatRef++));
                    webSocket.send(hb.toString());
                } catch (Exception e) {
                    Log.w(TAG, "Heartbeat failed: " + e.getMessage());
                }
                mainHandler.postDelayed(this, 25000);
            }
        }
    };

    public interface NotificationListener {
        void onNotificationReceived(FoodHeroNotification notification);
    }

    public SupabaseRealtimeClient() {
        this(null);
    }

    public SupabaseRealtimeClient(Context context) {
        this.context = (context != null) ? context.getApplicationContext() : null;
        this.client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    public void subscribe(String userId, NotificationListener listener) {
        this.currentUserId = userId;
        this.listener = listener;

        if (isSubscribed && webSocket != null) return;
        connectWebSocket();
    }

    private void connectWebSocket() {
        try {
            String wsUrl = SupabaseConfig.SUPABASE_URL.replace("https://", "wss://")
                + "/realtime/v1/websocket?apikey=" + SupabaseConfig.SUPABASE_ANON_KEY + "&vsn=1.0.0";

            Request request = new Request.Builder().url(wsUrl).build();
            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, Response response) {
                    Log.d(TAG, "Supabase Realtime connected successfully");
                    isSubscribed = true;

                    // Send Phoenix join message to subscribe to PostgreSQL changes on notifications
                    JsonObject joinMsg = new JsonObject();
                    joinMsg.addProperty("topic", "realtime:public:notifications");
                    joinMsg.addProperty("event", "phx_join");

                    JsonObject payload = new JsonObject();
                    JsonObject config = new JsonObject();
                    JsonObject change = new JsonObject();
                    change.addProperty("event", "INSERT");
                    change.addProperty("schema", "public");
                    change.addProperty("table", "notifications");

                    com.google.gson.JsonArray changesArray = new com.google.gson.JsonArray();
                    changesArray.add(change);
                    config.add("postgres_changes", changesArray);
                    payload.add("config", config);

                    joinMsg.add("payload", payload);
                    joinMsg.addProperty("ref", "join_notifications");
                    ws.send(joinMsg.toString());

                    // Start 25s heartbeat loop
                    mainHandler.removeCallbacks(heartbeatRunnable);
                    mainHandler.postDelayed(heartbeatRunnable, 25000);
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    try {
                        JsonObject obj = gson.fromJson(text, JsonObject.class);
                        if (obj == null) return;

                        String event = obj.has("event") ? obj.get("event").getAsString() : "";
                        if ("postgres_changes".equals(event) || "INSERT".equalsIgnoreCase(event)) {
                            handlePostgresChange(obj);
                        } else if (obj.has("payload")) {
                            JsonObject payload = obj.getAsJsonObject("payload");
                            if (payload != null && (payload.has("data") || payload.has("record"))) {
                                handlePostgresChange(obj);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Supabase realtime packet: " + e.getMessage());
                    }
                }

                @Override
                public void onClosed(WebSocket ws, int code, String reason) {
                    Log.d(TAG, "Supabase Realtime closed: " + reason);
                    isSubscribed = false;
                    mainHandler.removeCallbacks(heartbeatRunnable);
                }

                @Override
                public void onFailure(WebSocket ws, Throwable t, Response response) {
                    Log.w(TAG, "Supabase Realtime connection failure: " + t.getMessage());
                    isSubscribed = false;
                    mainHandler.removeCallbacks(heartbeatRunnable);

                    // Reconnect attempt after 5s if still active
                    mainHandler.postDelayed(() -> {
                        if (listener != null && !isSubscribed) {
                            Log.d(TAG, "Attempting Supabase Realtime reconnect...");
                            connectWebSocket();
                        }
                    }, 5000);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Supabase Realtime client", e);
        }
    }

    private void handlePostgresChange(JsonObject rootObj) {
        try {
            JsonObject payload = rootObj.has("payload") ? rootObj.getAsJsonObject("payload") : null;
            if (payload == null) return;

            JsonObject record = null;
            if (payload.has("data") && payload.get("data").isJsonObject()) {
                JsonObject data = payload.getAsJsonObject("data");
                if (data.has("record") && data.get("record").isJsonObject()) {
                    record = data.getAsJsonObject("record");
                }
            } else if (payload.has("record") && payload.get("record").isJsonObject()) {
                record = payload.getAsJsonObject("record");
            }

            if (record == null) return;

            FoodHeroNotification notification = gson.fromJson(record, FoodHeroNotification.class);
            if (notification == null) return;

            // Role / User filtering
            if (currentUserId != null && !currentUserId.isEmpty()) {
                String recipient = notification.getRecipientId();
                if (recipient != null && !recipient.equals(currentUserId)) {
                    return; // Belongs to another user
                }
            }

            // Dispatch in-app UI update
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onNotificationReceived(notification);
                }
            });

            // Trigger local Android heads-up notification (No FCM)
            showSystemNotification(notification);

        } catch (Exception e) {
            Log.e(TAG, "Failed to handle postgres change", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                    REALTIME_CHANNEL_ID,
                    "FoodHero Realtime Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Live updates from Supabase Realtime WebSocket (Without FCM)");
                channel.enableVibration(true);
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void showSystemNotification(FoodHeroNotification notif) {
        if (context == null) return;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REALTIME_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_foodhero_logo)
                .setContentTitle(notif.getTitle() != null ? notif.getTitle() : "FoodHero Alert")
                .setContentText(notif.getMessage() != null ? notif.getMessage() : "")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

            nm.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception e) {
            Log.w(TAG, "Error posting system notification: " + e.getMessage());
        }
    }

    public void unsubscribe() {
        mainHandler.removeCallbacks(heartbeatRunnable);
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Client unsubscribe");
            } catch (Exception ignored) {}
            webSocket = null;
        }
        isSubscribed = false;
        listener = null;
    }
}
