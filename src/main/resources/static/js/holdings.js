renderNavbar("holdings");

const CASH_SYMBOL = "USD_CASH";
const CASH_CATEGORY_ID = 3;
const SUPPORTED_TICKERS = new Set(["C", "AMZN", "TSLA", "FB", "AAPL"]);
const CASH_SYMBOLS = new Set(["CNY_CASH", "USD_CASH", "EUR_CASH", "INR_CASH", "GBP_CASH", "JPY_CASH", "KRW_CASH"]);

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
    if (categoryId === CASH_CATEGORY_ID) {
        const selectedCashSymbol = (el("cashCurrencySelect").value || CASH_SYMBOL).trim().toUpperCase();
        return CASH_SYMBOLS.has(selectedCashSymbol) ? selectedCashSymbol : CASH_SYMBOL;
    }

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
    const isCashCategory = categoryId === CASH_CATEGORY_ID;
    const isStockCategory = stockCategoryId !== null && categoryId === stockCategoryId;
    const symbolInput = el("symbol");
    const stockSelect = el("stockTickerSelect");
    const stockOther = el("stockTickerOther");
    const cashSelect = el("cashCurrencySelect");

    if (isCashCategory) {
        symbolInput.style.display = "none";
        symbolInput.required = false;
        stockSelect.style.display = "none";
        stockSelect.required = false;
        stockSelect.value = "";
        stockOther.style.display = "none";
        stockOther.value = "";
        cashSelect.style.display = "";
        cashSelect.required = true;
        return;
    }

    cashSelect.style.display = "none";
    cashSelect.required = false;

    if (isStockCategory) {
        symbolInput.style.display = "none";
        symbolInput.required = false;
        stockSelect.style.display = "";
        stockSelect.required = true;
        return;
    }

    symbolInput.style.display = "";
    symbolInput.required = true;
    stockSelect.style.display = "none";
    stockSelect.required = false;
    stockSelect.value = "";
    stockOther.style.display = "none";
    stockOther.value = "";
}


async function updateCurrentPricePreview() {
    const symbol = getSelectedSymbolForAdd();
    const input = el("currentPrice");
    const purchasePriceInput = el("purchasePrice");

    const categoryId = Number(el("categoryId").value || 0);
    if (categoryId === CASH_CATEGORY_ID) {
        try {
            const purchaseDate = el("purchaseDate").value || "";
            const [latestFx, purchaseFx] = await Promise.all([
                Api.getFxRate(symbol),
                Api.getFxRate(symbol, purchaseDate)
            ]);
            const latestUsdRate = Number(latestFx.usdRate || 0);
            const purchaseUsdRate = Number(purchaseFx.usdRate || 0);
            if (!latestUsdRate || latestUsdRate <= 0 || !purchaseUsdRate || purchaseUsdRate <= 0) {
                throw new Error("invalid FX rate");
            }
            input.readOnly = true;
            input.value = String(latestUsdRate);
            purchasePriceInput.readOnly = true;
            purchasePriceInput.value = String(purchaseUsdRate);
        } catch (error) {
            input.value = "";
            purchasePriceInput.value = "";
            showError("error", "Failed to preview FX rate: " + error.message);
        }
        return;
    }

    purchasePriceInput.readOnly = false;
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
    if (holding.categoryId === CASH_CATEGORY_ID) {
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

    const isCash = CASH_CATEGORY_ID === holding.categoryId;
    const cashCurrency = String(holding.symbol || "").replace("_CASH", "");
    el("tradePanelTitle").textContent = `${tradeTypeCode} ${holding.symbol}`;
    el("modalCategoryId").value = holding.categoryId || "";
    el("modalCategoryId").style.display = "none";
    el("modalSymbol").value = holding.symbol || "";
    el("modalTradeType").value = tradeTypeCode;
    el("modalTradeShares").value = "";
    el("modalTradeShares").placeholder = isCash ? `Amount (${cashCurrency})` : "Shares";
    el("modalTradePrice").value = String(Number(holding.currentPrice || 1));
    el("modalTradePrice").readOnly = isCash;
    el("modalTradeFee").value = "0";
    el("modalTradeDate").value = todayIsoDate();
    el("modalTradeNote").value = "";
    el("tradePanelError").style.display = "none";
    el("tradePanel").style.display = "flex";
    document.body.style.overflow = "hidden";
    if (isCash) {
        updateTradePricePreview().catch((error) => {
            showTradePanelError("Failed to preview FX rate: " + error.message);
        });
    }
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

async function updateTradePricePreview() {
    const holding = latestHoldings.find(h => h.id === currentTradeHoldingId);
    if (!holding || holding.categoryId !== CASH_CATEGORY_ID) {
        return;
    }

    const tradeDate = el("modalTradeDate").value || todayIsoDate();
    const fx = await Api.getFxRate(holding.symbol, tradeDate);
    const usdRate = Number(fx.usdRate || 0);
    if (!usdRate || usdRate <= 0) {
        throw new Error("invalid FX rate");
    }
    el("modalTradePrice").value = String(usdRate);
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
        holdingId: currentTradeHoldingId,
        assetSymbol: (holding.symbol || "").toUpperCase(),
        tradeTypeCode: currentTradeTypeCode,
        categoryId: Number(el("modalCategoryId").value),
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

    await updateCurrentPricePreview();
}

async function handleCashSymbolChange() {
    await updateCurrentPricePreview();
}

async function handlePurchaseDateChange() {
    await updateCurrentPricePreview();
}

async function handleTradeDateChange() {
    try {
        await updateTradePricePreview();
    } catch (error) {
        showTradePanelError("Failed to preview FX rate: " + error.message);
    }
}

async function handleSymbolInputChange() {
    await updateCurrentPricePreview();
}

function enableNativeDatePicker(inputId) {
    const input = el(inputId);
    if (!input) {
        return;
    }

    const openPicker = () => {
        if (typeof input.showPicker === "function") {
            try {
                input.showPicker();
            } catch (error) {
                // Ignore browser restrictions and keep native input behavior.
            }
        }
    };

    input.addEventListener("focus", openPicker);
    input.addEventListener("click", openPicker);
}

function bindEvents() {
    el("addHoldingForm").addEventListener("submit", handleAddHoldingSubmit);
    el("tradePanelForm").addEventListener("submit", handleTradeSubmit);

    el("categoryId").addEventListener("change", handleCategoryChange);
    el("stockTickerSelect").addEventListener("change", handleTickerSelectChange);
    el("cashCurrencySelect").addEventListener("change", handleCashSymbolChange);
    el("purchaseDate").addEventListener("change", handlePurchaseDateChange);
    el("modalTradeDate").addEventListener("change", handleTradeDateChange);
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
    enableNativeDatePicker("purchaseDate");
    enableNativeDatePicker("modalTradeDate");
    bindEvents();

    await loadCategories();
    await loadHoldings();
    await loadRecentTrades();
}

window.tradeForHolding = tradeForHolding;
window.deleteHolding = deleteHolding;

initPage();
