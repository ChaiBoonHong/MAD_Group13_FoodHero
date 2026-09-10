import { createClient } from "https://esm.sh/@supabase/supabase-js@2.57.4";

Deno.serve(async (request) => {
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });
  const url = Deno.env.get("SUPABASE_URL");
  const anon = Deno.env.get("SUPABASE_ANON_KEY");
  const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const resend = Deno.env.get("RESEND_API_KEY");
  const from = Deno.env.get("VERIFICATION_FROM_EMAIL");
  if (!url || !anon || !service || !resend || !from) return json({ error: "Verification service is not configured" }, 503);

  const token = request.headers.get("Authorization") ?? "";
  const userClient = createClient(url, anon, { global: { headers: { Authorization: token } } });
  const { data: { user } } = await userClient.auth.getUser();
  if (!user) return json({ error: "Authentication required" }, 401);

  const body = await request.json().catch(() => ({}));
  const email = String(body.email ?? "").trim().toLowerCase();
  const code = String(crypto.getRandomValues(new Uint32Array(1))[0] % 1_000_000).padStart(6, "0");
  const admin = createClient(url, service);
  const { error } = await admin.rpc("issue_institution_verification", { p_user_id: user.id, p_email: email, p_code: code });
  if (error) return json({ error: error.message }, 400);

  const sent = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${resend}`, "Content-Type": "application/json" },
    body: JSON.stringify({ from, to: [email], subject: "FoodHero institutional verification code",
      text: `Your FoodHero verification code is ${code}. It expires in 10 minutes.` }),
  });
  if (!sent.ok) return json({ error: "Verification email could not be delivered" }, 502);
  return json({ delivered: true }, 200);
});

function json(value: unknown, status: number) {
  return new Response(JSON.stringify(value), { status, headers: { "Content-Type": "application/json" } });
}
