function renderNavbar(active) {
    const links = [
        { href: "index.html", label: "Dashboard", key: "dashboard" },
        { href: "holdings.html", label: "Holdings", key: "holdings" },
        { href: "categories.html", label: "Asset Categories", key: "categories" },
        { href: "performance.html", label: "Performance", key: "performance" }
        { href: "holdings.html", label: "Holdings", key: "holdings" }
    ];

    const nav = document.createElement("div");
    nav.className = "navbar";
    nav.innerHTML =
        `<span class="brand">Portfolio Manager</span>` +
        links.map(l =>
            `<a href="${l.href}" class="${l.key === active ? "active" : ""}">${l.label}</a>`
        ).join("");

    document.body.prepend(nav);
}

