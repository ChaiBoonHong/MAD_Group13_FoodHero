import { createClient } from "https://esm.sh/@supabase/supabase-js@2.57.4";

const GOOGLE_ROUTES_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";
const FIELD_MASK = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline";

Deno.serve(async (request) => {
  if (request.method !== "POST") return response({ error: "Method not allowed" }, 405);
  const token = request.headers.get("Authorization") ?? "";
  const url = Deno.env.get("SUPABASE_URL");
  const anon = Deno.env.get("SUPABASE_ANON_KEY");
  if (!url || !anon) return response({ error: "Route service is not configured" }, 503);
  const userClient = createClient(url, anon, { global: { headers: { Authorization: token } } });
  const { data: { user } } = await userClient.auth.getUser();
  if (!user) return response({ error: "Authentication required" }, 401);

  const apiKey = Deno.env.get("GOOGLE_ROUTES_API_KEY");
  if (!apiKey) return response({ error: "Route service is not configured" }, 503);

  try {
    const body = await request.json();
    const allowedModes = new Set(["WALK", "BICYCLE", "DRIVE"]);
    if (!validWaypoint(body.origin) || !validWaypoint(body.destination)
        || !allowedModes.has(body.travelMode)) {
      return response({ error: "Invalid route request" }, 400);
    }

    const googleResponse = await fetch(GOOGLE_ROUTES_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Goog-Api-Key": apiKey,
        "X-Goog-FieldMask": FIELD_MASK,
      },
      body: JSON.stringify({
        origin: body.origin,
        destination: body.destination,
        travelMode: body.travelMode,
        computeAlternativeRoutes: false,
        languageCode: "en-US",
        units: "METRIC",
      }),
    });
    const result = await googleResponse.json();
    if (!googleResponse.ok) {
      console.error("Google Routes error", googleResponse.status, result?.error?.status);
      return response({ error: "Google route could not be calculated" }, googleResponse.status);
    }
    return response(result, 200);
  } catch (error) {
    console.error("google-routes failure", error);
    return response({ error: "Invalid route request" }, 400);
  }
});

function validWaypoint(value: any): boolean {
  const lat = value?.location?.latLng?.latitude;
  const lng = value?.location?.latLng?.longitude;
  return Number.isFinite(lat) && Number.isFinite(lng)
    && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
}

function response(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
