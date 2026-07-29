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
            const contentType = response.headers.get("content-type") || "";
            let errorMessage = "";
            let errorData = {};

            try {
                if (contentType.includes("application/json")) {
                    errorData = await response.json();
                    errorMessage = errorData.message || "Unknown error";
                    if (errorData.error) {
                        errorMessage += ` [${errorData.error}]`;
                    }
                } else {
                    errorMessage = await response.text().catch(() => "");
                }
            } catch (e) {
                errorMessage = "Failed to parse error response";
            }

            throw new Error(`Request to ${path} failed (${response.status}): ${errorMessage}`);
        }
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            return response.json();
        }
        return response.text();
    }

    return {
        // Dashboard
        getDashboard: () => request("/api/dashboard"),
        getAllocation: () => request("/api/allocation"),

        // Holdings
        getHoldings: (categoryId = "") => {
            if (categoryId === "" || categoryId === null || categoryId === undefined) {
                return request("/api/holdings");
            }
            return request(`/api/holdings?categoryId=${encodeURIComponent(categoryId)}`);
        },
        addHolding: (holding) => request("/api/holdings", { method: "POST", body: JSON.stringify(holding) }),
        refreshHoldingPrices: () => request("/api/holdings/refresh-prices", { method: "POST" }),
        getPriceSeries: (ticker) => request(`/api/holdings/price-series?ticker=${encodeURIComponent(ticker)}`),
        deleteHolding: (id) => request(`/api/holdings/${id}`, { method: "DELETE" }),
        tradeHolding: (trade) => request("/api/holdings/trade", { method: "POST", body: JSON.stringify(trade) }),
        getRecentTrades: (limit = 30) => request(`/api/holdings/trades?limit=${encodeURIComponent(limit)}`),

        // Asset categories
        getCategories: () => request("/api/categories"),
        addCategory: (category) => request("/api/categories", { method: "POST", body: JSON.stringify(category) }),
        deleteCategory: (id) => request(`/api/categories/${id}`, { method: "DELETE" }),

        // Performance
        getPerformance: (range) => request(`/api/performance${range ? `?range=${range}` : ""}`),
        getPerformanceCurve: () => request("/api/performance/curve"),
        getRanking: () => request("/api/performance/ranking")
    };
})();

function formatCurrency(value) {
    const num = Number(value ?? 0);
    return num.toLocaleString("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 2 });
}

function formatPercent(value, alreadyPercent = false) {
    const num = Number(value ?? 0) * (alreadyPercent ? 1 : 100);
    const sign = num > 0 ? "+" : "";
    return `${sign}${num.toFixed(2)}%`;
}

function showError(containerId, message) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.textContent = message;
    el.style.display = "block";
}

function clearError(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.textContent = "";
    el.style.display = "none";
}

