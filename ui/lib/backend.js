export async function requestBackend(baseUrl, path, options = {}) {
  if (!baseUrl) {
    throw new Error("Backend URL is not configured");
  }
  const url = new URL(path, baseUrl);
  return fetch(url, {
    ...options,
    cache: "no-store",
    signal: AbortSignal.timeout(5000),
  });
}

export async function getCatalog() {
  const response = await requestBackend(process.env.CATALOG_API_URL, "/api/catalog");
  if (!response.ok) {
    throw new Error(`Catalog request failed: ${response.status}`);
  }
  return response.json();
}
