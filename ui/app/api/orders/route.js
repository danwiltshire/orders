import { requestBackend } from "../../../lib/backend.js";

export async function POST(request) {
  try {
    const body = await request.text();
    const response = await requestBackend(process.env.ORDERS_API_URL, "/api/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body,
    });
    return new Response(await response.text(), {
      status: response.status,
      headers: { "Content-Type": response.headers.get("content-type") || "application/json" },
    });
  } catch (error) {
    console.error("Could not create order:", error);
    return Response.json({ error: "Orders service unavailable" }, { status: 503 });
  }
}
