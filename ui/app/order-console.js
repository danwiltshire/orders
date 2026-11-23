"use client";

import { useState } from "react";

const icons = ["▤", "✎", "◐"];
const formatPrice = (cents) => new Intl.NumberFormat("en-GB", { style: "currency", currency: "GBP" }).format(cents / 100);

async function readOrder(response) {
  if (!response.ok) {
    throw new Error(response.status === 404 ? "Order or product not found." : `Request failed (${response.status}). Please try again.`);
  }
  return response.json();
}

export default function OrderConsole({ products }) {
  const [selected, setSelected] = useState(products[0]?.id ?? null);
  const [quantity, setQuantity] = useState(1);
  const [order, setOrder] = useState(null);
  const [lookup, setLookup] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function createOrder(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setOrder(null);
    try {
      const response = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productId: selected, quantity: Number(quantity) }),
      });
      const result = await readOrder(response);
      setOrder(result);
      setLookup(result.id);
    } catch (failure) {
      setError(failure.message || "Unable to place order.");
    } finally {
      setBusy(false);
    }
  }

  async function findOrder(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setOrder(null);
    try {
      const response = await fetch(`/api/orders/${encodeURIComponent(lookup.trim())}`, { cache: "no-store" });
      setOrder(await readOrder(response));
    } catch (failure) {
      setError(failure.message || "Unable to find order.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="shop-layout">
      <form className="products" onSubmit={createOrder}>
        {products.length === 0 && <p className="notice">There are no products in the catalog yet.</p>}
        {products.map((product, index) => (
          <label className={`product ${selected === product.id ? "selected" : ""}`} key={product.id}>
            <input type="radio" name="product" value={product.id} checked={selected === product.id} onChange={() => setSelected(product.id)} />
            <span className={`product-art art-${index % 3}`} aria-hidden="true"><span>{icons[index % icons.length]}</span><small>NO. {String(index + 1).padStart(2, "0")}</small></span>
            <span className="product-info"><span className="product-name">{product.name}</span><span className="product-price">{formatPrice(product.priceCents)}</span></span>
            <span className="product-select">{selected === product.id ? "Selected ✓" : "Select item ↗"}</span>
          </label>
        ))}
        <div className="checkout">
          <label htmlFor="quantity">Quantity</label>
          <input id="quantity" type="number" min="1" max="100" required value={quantity} onChange={(event) => setQuantity(event.target.value)} />
          <button type="submit" disabled={busy || selected === null}>{busy ? "One moment…" : "Place order"} <span aria-hidden="true">→</span></button>
        </div>
      </form>
      <aside className="order-panel">
        <p className="eyebrow">YOUR ORDER</p>
        <h3>Good things<br />are on their way.</h3>
        <p>Every order gets its own ID. Keep it handy to check on its progress.</p>
        <form className="lookup" onSubmit={findOrder}>
          <label htmlFor="order-id">Order ID</label>
          <input id="order-id" value={lookup} onChange={(event) => setLookup(event.target.value)} placeholder="Paste an order ID" required />
          <button type="submit" disabled={busy}>Check status <span aria-hidden="true">↗</span></button>
        </form>
        {error && <p className="error" role="alert">{error}</p>}
        {order && <div className="order-result" aria-live="polite">
          <span className="status">{order.status}</span>
          <p>Order <strong>{order.id}</strong></p>
          <p>Quantity {order.quantity} · {formatPrice(order.unitPriceCents * order.quantity)}</p>
          {order.status === "PENDING" && <p>Processing has started. Check status again shortly.</p>}
        </div>}
      </aside>
    </div>
  );
}
