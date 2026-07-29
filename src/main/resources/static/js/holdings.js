renderNavbar("holdings");

const CASH_SYMBOL = "USD_CASH";
const SUPPORTED_TICKERS = new Set(["C", "AMZN", "TSLA", "FB", "AAPL"]);

let stockCategoryId = null;
let latestHoldings = [];
let currentTradeHoldingId = null;
let currentTradeTypeCode = null;

function el(id) {
    return document.getElementById(id);
}

function todayIsoDate() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function getSelectedSymbolForAdd() {
    const categoryId = Number(el("categoryId").value || 0);
    const isStockCategory = stockCategoryId !== null && categoryId === stockCategoryId;

    if (!isStockCategory) {
        return (el("symbol").value || "").trim().toUpperCase();
    }

    const selected = (el("stockTickerSelect").value || "").trim().toUpperCase();
    if (selected === "__OTHER__") {
        return (el("stockTickerOther").value || "").trim().toUpperCase();
    }
    return selected;
}

function updateSymbolInputsByCategory() {
    const categoryId = Number(el("categoryId").value || 0);
    const isStockCategory = stockCategoryId !== null && categoryId === stockCategoryId;

    if (isStockCategory) {
        el("symbol").style.display = "none";
        el("symbol").required = false;
        el("stockTickerSelect").style.display = "";
        el("stockTickerSelect").required = true;
        return;
    }

    el("symbol").style.display = "";
    el("symbol").required = true;
    el("stockTickerSelect").style.display = "none";
    el("stockTickerSelect").required = false;
    el("stockTickerSelect").value = "";
    el("stockTickerOther").style.display = "none";
    el("stockTickerOther").value = "";
}

function updateSharesPlaceholder() {
    const symbol = getSelectedSymbolForAdd();
    el("shares").placeholder = symbol === CASH_SYMBOL ? "Amount (USD)" : "Shares";
}

async function updateCurrentPricePreview() {
    const symbol = getSelectedSymbolForAdd();
    const input = el("currentPrice");

    if (symbol === CASH_SYMBOL) {
        input.readOnly = true;
        input.value = "0.01";
        return;
    }

    if (SUPPORTED_TICKERS.has(symbol)) {
        input.readOnly = true;
        input.value = "";
        try {
            const priceSeries = await Api.getPriceSeries(symbol);
            const closes = Array.isArray(priceSeries && priceSeries.close) ? priceSeries.close : [];
            if (closes.length > 0) {
                input.value = String(closes[closes.length - 1]);
            }
        } catch (error) {
            input.value = "";
            showError("error", "Failed to preview current price: " + error.message);
        }
        return;
    }

    input.readOnly = false;
    input.placeholder = "Current Price (Manual for other symbols)";
    input.value = "";
}

function getTradeButtons(holding) {
    if (holding.cashAsset) {
        return `
            <button class="secondary trade-action-btn" onclick="tradeForHolding(${holding.id}, 'DEPOSIT')">Deposit</button>
            <button class="secondary trade-action-btn" onclick="tradeForHolding(${holding.id}, 'WITHDRAW')">Withdraw</button>
        `;
    }

    return `
        <button class="secondary trade-action-btn" onclick="tradeForHolding(${holding.id}, 'BUY')">Buy</button>
        <button class="secondary trade-action-btn" onclick="tradeForHolding(${holding.id}, 'SELL')">Sell</button>
    `;
}

function renderHoldingsTable(holdings) {
    if (!holdings.length) {
        el("holdingsBody").innerHTML = "";
        el("holdingsEmpty").style.display = "block";
        return;
    }

    el("holdingsEmpty").style.display = "none";
    el("holdingsBody").innerHTML = holdings.map(h => {
        const pl = Number(h.profitLoss || 0);
        return `
            <tr>
                <td>${h.symbol || ""}</td>
                <td>${h.companyName || ""}</td>
                <td>${h.categoryName || ""}</td>
                <td>${h.displayShares ?? ""}</td>
                <td>${formatCurrency(h.purchasePrice)}</td>
                <td>${formatCurrency(h.currentPrice)}</td>
                <td>${formatCurrency(h.marketValue)}</td>
                <td class="${pl >= 0 ? "value positive" : "value negative"}">${pl >= 0 ? "+" : ""}${formatCurrency(h.profitLoss)}</td>
                <td class="action-cell">
                    ${getTradeButtons(h)}
                    <button class="danger trade-action-btn" onclick="deleteHolding(${h.id})">Delete</button>
                </td>
            </tr>
        `;
    }).join("");
}

function renderTradesTable(trades) {
    if (!trades.length) {
        el("tradeBody").innerHTML = "";
        el("tradeEmpty").style.display = "block";
        return;
    }

    el("tradeEmpty").style.display = "none";
    el("tradeBody").innerHTML = trades.map(t => {
        const cashClass = Number(t.cashChange || 0) >= 0 ? "value positive" : "value negative";
        return `
            <tr>
                <td>${t.tradeDate || ""}</td>
                <td>${t.assetSymbol || ""}</td>
                <td>${t.tradeTypeCode || ""}</td>
                <td>${t.displayShares ?? ""}</td>
                <td>${formatCurrency(t.unitPrice)}</td>
                <td>${formatCurrency(t.tradeAmount)}</td>
                <td class="${cashClass}">${formatCurrency(t.cashChange)}</td>
                <td>${t.note || ""}</td>
            </tr>
        `;
    }).join("");
}

function openTradePanel(holding, tradeTypeCode) {
    currentTradeHoldingId = holding.id;
    currentTradeTypeCode = tradeTypeCode;

    const isCash = !!holding.cashAsset;
    el("tradePanelTitle").textContent = `${tradeTypeCode} ${holding.symbol}`;
    el("modalSymbol").value = holding.symbol || "";
    el("modalTradeType").value = tradeTypeCode;
    el("modalTradeShares").value = "";
    el("modalTradeShares").placeholder = isCash ? "Amount (USD)" : "Shares";
    el("modalTradePrice").value = String(isCash ? 0.01 : Number(holding.currentPrice || 1));
    el("modalTradePrice").readOnly = isCash;
    el("modalTradeFee").value = "0";
    el("modalTradeDate").value = todayIsoDate();
    el("modalTradeNote").value = "";
    el("tradePanelError").style.display = "none";
    el("tradePanel").style.display = "flex";
    document.body.style.overflow = "hidden";
}

function closeTradePanel() {
    el("tradePanel").style.display = "none";
    document.body.style.overflow = "";
    el("tradePanelError").style.display = "none";
    el("tradePanelError").textContent = "";
    el("tradePanelForm").reset();
    currentTradeHoldingId = null;
    currentTradeTypeCode = null;
}

function showTradePanelError(message) {
    el("tradePanelError").textContent = message;
    el("tradePanelError").style.display = "block";
}

async function loadCategories() {
    try {
        const categories = await Api.getCategories();

        el("categoryId").innerHTML = categories.map(c => `<option value="${c.id}">${c.categoryName}</option>`).join("");
        el("holdingCategoryFilter").innerHTML = [
            `<option value="">All Types</option>`,
            ...categories.map(c => `<option value="${c.id}">${c.categoryName}</option>`)
        ].join("");

        const stockCategory = categories.find(c => String(c.categoryName || "").toLowerCase() === "stock");
        stockCategoryId = stockCategory ? Number(stockCategory.id) : null;

        updateSymbolInputsByCategory();
        updateSharesPlaceholder();
        await updateCurrentPricePreview();
    } catch (error) {
        showError("error", "Failed to load asset categories: " + error.message);
    }
}

async function loadHoldings() {
    try {
        const categoryId = el("holdingCategoryFilter").value;
        latestHoldings = await Api.getHoldings(categoryId);
        renderHoldingsTable(latestHoldings);
    } catch (error) {
        showError("error", "Failed to load holdings: " + error.message);
    }
}

async function loadRecentTrades() {
    try {
        const trades = await Api.getRecentTrades(30);
        renderTradesTable(trades);
    } catch (error) {
        showError("error", "Failed to load trades: " + error.message);
    }
}

async function tradeForHolding(id, tradeTypeCode) {
    const holding = latestHoldings.find(h => h.id === id);
    if (!holding) {
        showError("error", "Holding not found.");
        return;
    }
    openTradePanel(holding, tradeTypeCode);
}

async function deleteHolding(id) {
    if (!confirm("Delete this holding?")) {
        return;
    }

    try {
        await Api.deleteHolding(id);
        await loadHoldings();
    } catch (error) {
        showError("error", "Failed to delete holding: " + error.message);
    }
}

async function refreshPrices() {
    const button = el("refreshPricesBtn");
    button.disabled = true;
    button.textContent = "Refreshing...";

    try {
        const result = await Api.refreshHoldingPrices();
        await loadHoldings();
        const skipped = (result.skippedUnsupported && result.skippedUnsupported.length) || 0;
        const failed = (result.failed && result.failed.length) || 0;
        alert(`Price refresh done. Updated rows: ${result.updatedRows}, skipped: ${skipped}, failed: ${failed}`);
    } catch (error) {
        showError("error", "Failed to refresh prices: " + error.message);
    } finally {
        button.disabled = false;
        button.textContent = "Refresh Prices";
    }
}

async function handleAddHoldingSubmit(event) {
    event.preventDefault();
    clearError("error");

    const currentPriceRaw = el("currentPrice").value;
    const payload = {
        symbol: getSelectedSymbolForAdd(),
        categoryId: Number(el("categoryId").value),
        shares: Number(el("shares").value),
        purchasePrice: Number(el("purchasePrice").value),
        currentPrice: currentPriceRaw === "" ? null : Number(currentPriceRaw),
        purchaseDate: el("purchaseDate").value
    };

    try {
        await Api.addHolding(payload);
        event.target.reset();
        el("stockTickerOther").style.display = "none";
        el("currentPrice").readOnly = true;
        el("currentPrice").placeholder = "Current Price (Auto from API)";
        updateSymbolInputsByCategory();
        await loadHoldings();
    } catch (error) {
        showError("error", "Failed to add holding: " + error.message);
    }
}

async function handleTradeSubmit(event) {
    event.preventDefault();

    const holding = latestHoldings.find(h => h.id === currentTradeHoldingId);
    if (!holding || !currentTradeTypeCode) {
        showTradePanelError("Trade context is invalid. Please retry.");
        return;
    }

    const payload = {
        assetSymbol: (holding.symbol || "").toUpperCase(),
        tradeTypeCode: currentTradeTypeCode,
        tradeShares: Number(el("modalTradeShares").value),
        tradePrice: Number(el("modalTradePrice").value),
        tradeDate: el("modalTradeDate").value || todayIsoDate(),
        fee: Number(el("modalTradeFee").value || 0),
        note: (el("modalTradeNote").value || "").trim()
    };

    try {
        await Api.tradeHolding(payload);
        closeTradePanel();
        await loadHoldings();
        await loadRecentTrades();
    } catch (error) {
        showTradePanelError("Failed to submit trade: " + error.message);
    }
}

async function handleCategoryChange() {
    updateSymbolInputsByCategory();
    updateSharesPlaceholder();
    await updateCurrentPricePreview();
}

async function handleTickerSelectChange() {
    const selected = (el("stockTickerSelect").value || "").trim().toUpperCase();

    if (selected === "__OTHER__") {
        el("stockTickerOther").style.display = "";
        el("stockTickerOther").focus();
    } else {
        el("stockTickerOther").style.display = "none";
        el("stockTickerOther").value = "";
    }

    updateSharesPlaceholder();
    await updateCurrentPricePreview();
}

async function handleSymbolInputChange() {
    updateSharesPlaceholder();
    await updateCurrentPricePreview();
}

function bindEvents() {
    el("addHoldingForm").addEventListener("submit", handleAddHoldingSubmit);
    el("tradePanelForm").addEventListener("submit", handleTradeSubmit);

    el("categoryId").addEventListener("change", handleCategoryChange);
    el("stockTickerSelect").addEventListener("change", handleTickerSelectChange);
    el("stockTickerOther").addEventListener("input", handleSymbolInputChange);
    el("symbol").addEventListener("input", handleSymbolInputChange);

    el("holdingCategoryFilter").addEventListener("change", loadHoldings);
    el("refreshPricesBtn").addEventListener("click", refreshPrices);
    el("reloadTradesBtn").addEventListener("click", loadRecentTrades);
    el("tradePanelCloseBtn").addEventListener("click", closeTradePanel);
    el("tradePanelCancelBtn").addEventListener("click", closeTradePanel);
    el("tradePanel").addEventListener("click", (event) => {
        if (event.target.id === "tradePanel") {
            closeTradePanel();
        }
    });
}

async function initPage() {
    el("purchaseDate").value = todayIsoDate();
    bindEvents();

    await loadCategories();
    await loadHoldings();
    await loadRecentTrades();
}

window.tradeForHolding = tradeForHolding;
window.deleteHolding = deleteHolding;

initPage();

