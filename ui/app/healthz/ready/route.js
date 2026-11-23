import { requestBackend } from "../../../lib/backend.js";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const checks = await Promise.all([
      requestBackend(process.env.CATALOG_API_URL, "/healthz/ready"),
      requestBackend(process.env.ORDERS_API_URL, "/healthz/ready"),
    ]);
    if (checks.every((response) => response.ok)) {
      return new Response("OK");
    }
    console.error("UI dependency readiness check failed:", checks.map((response) => response.status));
  } catch (error) {
    console.error("UI dependency readiness check failed:", error);
  }
  return new Response("Dependencies unavailable", { status: 503 });
}
