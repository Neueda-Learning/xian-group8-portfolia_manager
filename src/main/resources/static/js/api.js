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
        getHoldings: () => request("/api/holdings"),
        addHolding: (holding) => request("/api/holdings", { method: "POST", body: JSON.stringify(holding) }),
        deleteHolding: (id) => request(`/api/holdings/${id}`, { method: "DELETE" }),

        // Asset categories
        getCategories: () => request("/api/categories"),
        addCategory: (category) => request("/api/categories", { method: "POST", body: JSON.stringify(category) }),
        deleteCategory: (id) => request(`/api/categories/${id}`, { method: "DELETE" }),

        // Performance
        getPerformance: (range) => request(`/api/performance${range ? `?range=${range}` : ""}`),
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

