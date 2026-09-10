package com.uccd3223.group13.foodhero.data.remote;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.maps.android.PolyUtil;
import com.uccd3223.group13.foodhero.data.model.GeoPoint;
import com.uccd3223.group13.foodhero.data.model.RouteResult;
import com.uccd3223.group13.foodhero.data.model.TravelMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Minimal client for Google Routes API v2 computeRoutes. */
public final class GoogleRoutesClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final String authorization;

    public GoogleRoutesClient(String authorization) {
        this.authorization = authorization;
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
    }

    public RouteResult computeRoute(double originLat, double originLng,
                                    double destinationLat, double destinationLng,
                                    TravelMode mode) throws IOException {
        JsonObject payload = new JsonObject();
        payload.add("origin", waypoint(originLat, originLng));
        payload.add("destination", waypoint(destinationLat, destinationLng));
        payload.addProperty("travelMode", googleTravelMode(mode));
        payload.addProperty("computeAlternativeRoutes", false);
        payload.addProperty("languageCode", "en-US");
        payload.addProperty("units", "METRIC");

        Request request = new Request.Builder()
            .url(SupabaseConfig.SUPABASE_URL + "/functions/v1/google-routes")
            .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            .header("Authorization", authorization)
            .post(RequestBody.create(payload.toString(), JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Google Routes API returned HTTP " + response.code()
                    + apiErrorMessage(body));
            }
            return parseRoute(body, mode);
        }
    }

    private static JsonObject waypoint(double latitude, double longitude) {
        JsonObject latLng = new JsonObject();
        latLng.addProperty("latitude", latitude);
        latLng.addProperty("longitude", longitude);
        JsonObject location = new JsonObject();
        location.add("latLng", latLng);
        JsonObject waypoint = new JsonObject();
        waypoint.add("location", location);
        return waypoint;
    }

    private static String googleTravelMode(TravelMode mode) {
        if (mode == TravelMode.CYCLING) return "BICYCLE";
        if (mode == TravelMode.SHUTTLE) return "DRIVE";
        return "WALK";
    }

    private static RouteResult parseRoute(String body, TravelMode mode) throws IOException {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException parseFailure) {
            throw new IOException("Google Routes API returned malformed JSON", parseFailure);
        }
        JsonArray routes = root.getAsJsonArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new IOException("Google Routes API returned no route");
        }

        JsonObject route = routes.get(0).getAsJsonObject();
        if (!route.has("distanceMeters") || !route.has("duration")
            || !route.has("polyline")) {
            throw new IOException("Google Routes API response is missing required route fields");
        }
        String encoded = route.getAsJsonObject("polyline").get("encodedPolyline").getAsString();
        if (encoded == null || encoded.isEmpty()) {
            throw new IOException("Google Routes API returned an empty route polyline");
        }

        List<GeoPoint> points = new ArrayList<>();
        for (LatLng point : PolyUtil.decode(encoded)) {
            points.add(new GeoPoint(point.latitude, point.longitude));
        }
        if (points.size() < 2) {
            throw new IOException("Google Routes API returned an invalid route polyline");
        }

        RouteResult result = new RouteResult();
        result.setTravelMode(mode);
        result.setDistanceMeters(route.get("distanceMeters").getAsDouble());
        result.setDurationSeconds(parseDurationSeconds(route.get("duration").getAsString()));
        result.setEncodedPolyline(encoded);
        result.setPoints(points);
        return result;
    }

    private static int parseDurationSeconds(String duration) throws IOException {
        try {
            String seconds = duration.endsWith("s")
                ? duration.substring(0, duration.length() - 1) : duration;
            return Math.max(1, (int) Math.ceil(Double.parseDouble(seconds)));
        } catch (RuntimeException invalidDuration) {
            throw new IOException("Google Routes API returned an invalid duration", invalidDuration);
        }
    }

    private static String apiErrorMessage(String body) {
        try {
            JsonObject error = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error");
            if (error != null && error.has("message")) return ": " + error.get("message").getAsString();
        } catch (RuntimeException ignored) {
            // The HTTP status remains actionable when Google does not return its normal error shape.
        }
        return "";
    }
}
