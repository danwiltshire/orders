import { requestBackend } from "../../../../lib/backend.js";

export async function GET(_request, { params }) {
  try {
    const { id } = await params;
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)) {
      return Response.json({ error: "Invalid order ID" }, { status: 400 });
    }
    const response = await requestBackend(process.env.ORDERS_API_URL, `/api/orders/${id}`);
    return new Response(await response.text(), {
      status: response.status,
      headers: { "Content-Type": response.headers.get("content-type") || "application/json" },
    });
  } catch (error) {
    console.error("Could not retrieve order:", error);
    return Response.json({ error: "Orders service unavailable" }, { status: 503 });
  }
}
