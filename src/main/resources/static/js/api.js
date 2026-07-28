/**
 * Shared REST helper for the Portfolio Manager static front-end.
 * All backend endpoints are served from the same origin (server.port=9099),
 * so relative paths are used.
 */
const Api = (() => {
    async function request(path, options = {}) {
        const response = await fetch(path, {
            headers: { "Content-Type": "application/json" },
            ...options
        });
        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(`Request to ${path} failed (${response.status}): ${text}`);
        }
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            return response.json();
        }
        return response.text();
    }

    return {
        getHoldings: () => request("/api/holdings"),
        getCategories: () => request("/api/categories"),
        addHolding: (holding) => request("/api/holdings", { method: "POST", body: JSON.stringify(holding) }),
        refreshHoldingPrices: () => request("/api/holdings/refresh-prices", { method: "POST" }),
        getPriceSeries: (ticker) => request(`/api/holdings/price-series?ticker=${encodeURIComponent(ticker)}`),
        deleteHolding: (id) => request(`/api/holdings/${id}`, { method: "DELETE" })
    };
})();

function formatCurrency(value) {
    const num = Number(value ?? 0);
    return num.toLocaleString("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 2 });
}

function showError(containerId, message) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.textContent = message;
    el.style.display = "block";
}
