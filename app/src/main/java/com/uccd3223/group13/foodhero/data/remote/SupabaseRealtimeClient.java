package com.uccd3223.group13.foodhero.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SupabaseRealtimeClient {
    private static final String TAG = "SupabaseRealtime";
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    private WebSocket webSocket;
    private boolean isSubscribed = false;
    private NotificationListener listener;

    public interface NotificationListener {
        void onNotificationReceived(FoodHeroNotification notification);
    }

    public SupabaseRealtimeClient() {
        this.client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void subscribe(String userId, NotificationListener listener) {
        this.listener = listener;
        if (isSubscribed) return;

        try {
            String wsUrl = SupabaseConfig.SUPABASE_URL.replace("https://", "wss://") 
                + "/realtime/v1/websocket?apikey=" + SupabaseConfig.SUPABASE_ANON_KEY + "&vsn=1.0.0";

            Request request = new Request.Builder().url(wsUrl).build();
            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    Log.d(TAG, "Supabase Realtime connected");
                    isSubscribed = true;
                    // Send join message for notifications
                    JsonObject joinMsg = new JsonObject();
                    joinMsg.addProperty("topic", "realtime:public:notifications");
                    joinMsg.addProperty("event", "phx_join");
                    joinMsg.add("payload", new JsonObject());
                    joinMsg.addProperty("ref", "1");
                    webSocket.send(joinMsg.toString());
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        JsonObject obj = gson.fromJson(text, JsonObject.class);
                        if (obj != null && obj.has("payload")) {
                            JsonObject payload = obj.getAsJsonObject("payload");
                            if (payload != null && payload.has("data")) {
                                JsonObject data = payload.getAsJsonObject("data");
                                if (data != null && data.has("record")) {
                                    FoodHeroNotification notif = gson.fromJson(data.get("record"), FoodHeroNotification.class);
                                    if (notif != null && (userId == null || userId.equals(notif.getRecipientId()))) {
                                        mainHandler.post(() -> {
                                            if (SupabaseRealtimeClient.this.listener != null) {
                                                SupabaseRealtimeClient.this.listener.onNotificationReceived(notif);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse realtime notification payload", e);
                    }
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    isSubscribed = false;
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    Log.w(TAG, "Supabase Realtime socket error: " + t.getMessage());
                    isSubscribed = false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Supabase realtime client", e);
        }
    }

    public void unsubscribe() {
        if (webSocket != null) {
            webSocket.close(1000, "User logged out / destroyed");
            webSocket = null;
        }
        isSubscribed = false;
        listener = null;
    }
}
