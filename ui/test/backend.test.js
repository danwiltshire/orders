import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import { getCatalog } from "../lib/backend.js";
import { GET as readiness } from "../app/healthz/ready/route.js";
import { POST as createOrder } from "../app/api/orders/route.js";
import { GET as getOrder } from "../app/api/orders/[id]/route.js";

const originalFetch = globalThis.fetch;
const originalCatalog = process.env.CATALOG_API_URL;
const originalOrders = process.env.ORDERS_API_URL;

afterEach(() => {
  globalThis.fetch = originalFetch;
  if (originalCatalog === undefined) delete process.env.CATALOG_API_URL;
  else process.env.CATALOG_API_URL = originalCatalog;
  if (originalOrders === undefined) delete process.env.ORDERS_API_URL;
  else process.env.ORDERS_API_URL = originalOrders;
});

test("catalog fetch uses the configured backend and surfaces failures", async () => {
  process.env.CATALOG_API_URL = "http://catalog:8080";
  globalThis.fetch = async (url, options) => {
    assert.equal(url.href, "http://catalog:8080/api/catalog");
    assert.equal(options.cache, "no-store");
    return Response.json([{ id: 1, name: "Notebook", priceCents: 1299 }]);
  };
  assert.equal((await getCatalog())[0].name, "Notebook");
  globalThis.fetch = async () => new Response(null, { status: 503 });
  await assert.rejects(getCatalog(), /503/);
});

test("readiness reports failure if either dependency is unhealthy", async () => {
  process.env.CATALOG_API_URL = "http://catalog:8080";
  process.env.ORDERS_API_URL = "http://orders:8080";
  globalThis.fetch = async (url) => new Response(null, { status: url.hostname === "orders" ? 503 : 200 });
  assert.equal((await readiness()).status, 503);
  globalThis.fetch = async () => new Response(null, { status: 200 });
  assert.equal((await readiness()).status, 200);
});

test("order proxy preserves the request and backend status", async () => {
  process.env.ORDERS_API_URL = "http://orders:8080";
  globalThis.fetch = async (url, options) => {
    assert.equal(url.href, "http://orders:8080/api/orders");
    assert.equal(options.method, "POST");
    assert.deepEqual(JSON.parse(options.body), { productId: 1, quantity: 2 });
    return Response.json({ id: "created" }, { status: 201 });
  };
  const response = await createOrder(new Request("http://ui/api/orders", {
    method: "POST",
    body: JSON.stringify({ productId: 1, quantity: 2 }),
  }));
  assert.equal(response.status, 201);
  assert.equal((await response.json()).id, "created");
});

test("order lookup rejects invalid IDs without contacting the backend", async () => {
  globalThis.fetch = () => { throw new Error("unexpected backend request"); };
  const response = await getOrder(null, { params: Promise.resolve({ id: "../bad" }) });
  assert.equal(response.status, 400);
});
