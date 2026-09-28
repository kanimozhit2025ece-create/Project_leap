
"use strict";

// ==================== PAGE NAVIGATION ====================

const pages = document.querySelectorAll(".page");
const menuLinks = document.querySelectorAll(
    ".sidebar nav a[data-page]"
);

async function showPage(pageId) {
    pages.forEach(page => {
        page.hidden = page.id !== pageId;
    });

    menuLinks.forEach(link => {
        link.classList.toggle(
            "active",
            link.dataset.page === pageId
        );
    });

    if (pageId === "dashboard") await loadDashboard();
    if (pageId === "ambulances") await loadAmbulances();
    if (pageId === "add-ambulance") await loadZonesForForm();
    if (pageId === "emergency") await loadEmergencyCalls();
    if (pageId === "register") await loadEmergencyZones();
    if (pageId === "zones") await loadZoneTable();
    if (pageId === "distances") await loadDistancePage();
    if (pageId === "reports") initializeReportDates();
    if (pageId === "settings") await loadSettings();
}

menuLinks.forEach(link => {
    link.addEventListener("click", event => {
        event.preventDefault();
        showPage(link.dataset.page);
    });
});

document.getElementById("openAddAmbulance")
    ?.addEventListener("click", () => {
        showPage("add-ambulance");
    });


// ==================== COMMON FUNCTIONS ====================

async function getData(url) {
    const response = await fetch(url);

    if (response.status === 401) {
        window.location.replace("/login.html");
        throw new Error("Please login");
    }

    if (!response.ok) {
        throw new Error(await getErrorMessage(response));
    }

    return response.json();
}

async function getErrorMessage(response) {
    const fallback = "HTTP " + response.status;

    try {
        const data = await response.json();

        return data.detail ||
            data.message ||
            data.error ||
            fallback;

    } catch {
        return fallback;
    }
}

function addCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "-";
    row.appendChild(cell);
    return cell;
}

function addTableRow(table, values) {
    const row = document.createElement("tr");

    values.forEach(value => {
        addCell(row, value);
    });

    table.appendChild(row);
    return row;
}

function formatDate(value) {
    return value
        ? value.replace("T", " ").slice(0, 16)
        : "-";
}

function setMessage(id, text, type = "") {
    const element = document.getElementById(id);

    if (!element) return;

    element.textContent = text;
    element.className = type;
}


// ==================== DASHBOARD ====================

async function loadDashboard() {
    try {
        const [ambulances, calls] = await Promise.all([
            getData("/ambulance"),
            getData("/emergency")
        ]);

        document.getElementById("totalAmbulances")
            .textContent = ambulances.length;

        document.getElementById("availableAmbulances")
            .textContent = ambulances.filter(
            a => a.status === "AVAILABLE"
        ).length;

        document.getElementById("pendingCalls")
            .textContent = calls.filter(
            c => c.status === "PENDING"
        ).length;

        document.getElementById("completedCalls")
            .textContent = calls.filter(
            c => c.status === "COMPLETED"
        ).length;

        const table = document.getElementById(
            "emergencyTable"
        );

        table.replaceChildren();

        const recentCalls = [...calls]
            .sort((a, b) => b.id - a.id)
            .slice(0, 5);

        recentCalls.forEach(call => {
            addTableRow(table, [
                call.id,
                call.callerZone?.zoneName ?? "-",
                call.status,
                call.ambulance?.ambulanceNumber ?? "-",
                formatDate(call.receivedAt)
            ]);
        });

    } catch (error) {
        console.error(error);

        setMessage(
            "dashboardMessage",
            "Dashboard data load ஆகவில்லை.",
            "error"
        );
    }
}


// ==================== AMBULANCES ====================

async function loadAmbulances() {
    try {
        const ambulances = await getData("/ambulance");

        const table = document.getElementById(
            "ambulanceTable"
        );

        table.replaceChildren();

        ambulances.forEach(ambulance => {
            const row = document.createElement("tr");

            [
                ambulance.id,
                ambulance.ambulanceNumber,
                ambulance.homeZone?.zoneName ?? "-",
                ambulance.currentZone?.zoneName ?? "-"
            ].forEach(value => addCell(row, value));

            const statusCell = document.createElement("td");
            const badge = document.createElement("span");

            badge.textContent = ambulance.status;

            badge.className =
                "status " +
                (ambulance.status === "AVAILABLE"
                    ? "available"
                    : "busy");

            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            table.appendChild(row);
        });

    } catch (error) {
        console.error(error);
        alert("Ambulance list load ஆகவில்லை.");
    }
}

async function fillZoneSelect(selectId) {
    const select = document.getElementById(selectId);

    if (!select) return;

    select.replaceChildren();

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "Select Zone";

    select.appendChild(placeholder);

    const zones = await getData("/zone");

    zones.forEach(zone => {
        const option = document.createElement("option");

        option.value = zone.id;
        option.textContent = zone.zoneName;

        select.appendChild(option);
    });
}

async function loadZonesForForm() {
    try {
        await fillZoneSelect("homeZone");
        setMessage("formMessage", "");

    } catch (error) {
        setMessage(
            "formMessage",
            "Zones load ஆகவில்லை.",
            "error"
        );
    }
}

document.getElementById("ambulanceForm")
    ?.addEventListener("submit", async event => {
        event.preventDefault();

        const number = document.getElementById(
            "ambulanceNumber"
        ).value.trim();

        const zoneId = Number(
            document.getElementById("homeZone").value
        );

        const button = document.getElementById(
            "saveAmbulance"
        );

        if (!number || !zoneId) {
            setMessage(
                "formMessage",
                "Ambulance number மற்றும் home zone கொடு.",
                "error"
            );
            return;
        }

        button.disabled = true;
        setMessage("formMessage", "Saving...");

        try {
            const response = await fetch("/ambulance", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    ambulanceNumber: number,
                    homeZone: {
                        id: zoneId
                    }
                })
            });

            if (!response.ok) {
                throw new Error(
                    await getErrorMessage(response)
                );
            }

            event.target.reset();

            await showPage("ambulances");

        } catch (error) {
            setMessage(
                "formMessage",
                error.message,
                "error"
            );

        } finally {
            button.disabled = false;
        }
    });


// ==================== EMERGENCY REGISTRATION ====================

async function loadEmergencyZones() {
    try {
        await fillZoneSelect("callerZone");
        setMessage("emergencyMessage", "");

    } catch (error) {
        setMessage(
            "emergencyMessage",
            "Zones load ஆகவில்லை.",
            "error"
        );
    }
}

document.getElementById("emergencyForm")
    ?.addEventListener("submit", async event => {
        event.preventDefault();

        const zoneId = document.getElementById(
            "callerZone"
        ).value;

        const priority = document.getElementById(
            "emergencyPriority"
        ).value;

        const button = document.getElementById(
            "registerButton"
        );

        if (!zoneId) {
            setMessage(
                "emergencyMessage",
                "Please select a zone.",
                "error"
            );
            return;
        }

        if (!["NORMAL", "HIGH", "CRITICAL"]
            .includes(priority)) {

            setMessage(
                "emergencyMessage",
                "Invalid priority.",
                "error"
            );
            return;
        }

        button.disabled = true;

        setMessage(
            "emergencyMessage",
            "Registering..."
        );

        try {
            const params = new URLSearchParams({
                priority
            });

            const response = await fetch(
                "/emergency/" +
                encodeURIComponent(zoneId) +
                "?" +
                params.toString(),
                {
                    method: "POST"
                }
            );

            if (!response.ok) {
                throw new Error(
                    await getErrorMessage(response)
                );
            }

            const call = await response.json();

            setMessage(
                "emergencyMessage",
                "Emergency Call #" +
                call.id +
                " registered successfully!",
                "success"
            );

            event.target.reset();

        } catch (error) {
            setMessage(
                "emergencyMessage",
                error.message,
                "error"
            );

        } finally {
            button.disabled = false;
        }
    });


// ==================== EMERGENCY CALLS ====================

// Load saved zone distances.

async function getDistanceMap() {
    const distances = await getData("/distance");

    const map = new Map();

    distances.forEach(distance => {
        const fromId = distance.fromZone?.id;
        const toId = distance.toZone?.id;

        if (fromId != null && toId != null) {
            map.set(
                `${fromId}-${toId}`,
                Number(distance.distanceKm)
            );
        }
    });

    return map;
}

// Current distance shown in the table.

function getDisplayedDistance(call, distanceMap) {
    if (!call.ambulance) {
        return "-";
    }

    const fromId = call.ambulance.currentZone?.id;
    const toId = call.callerZone?.id;

    if (fromId == null || toId == null) {
        return "Unknown";
    }

    if (fromId === toId) {
        return "0 km";
    }

    const distance = distanceMap.get(
        `${fromId}-${toId}`
    );

    return distance == null
        ? "Not available"
        : `${distance} km`;
}

async function loadEmergencyCalls() {
    const table = document.getElementById(
        "allEmergencyTable"
    );

    if (!table) return;

    try {
        const [calls, distanceMap] = await Promise.all([
            getData("/emergency"),
            getDistanceMap()
        ]);

        table.replaceChildren();

        const sortedCalls = [...calls].sort(
            (a, b) => b.id - a.id
        );

        if (sortedCalls.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");

            cell.colSpan = 7;
            cell.textContent = "No emergency calls found.";

            row.appendChild(cell);
            table.appendChild(row);

            return;
        }

        sortedCalls.forEach(call => {
            const row = document.createElement("tr");

            // CALL ID
            addCell(row, call.id);

            // CALLER ZONE
            addCell(
                row,
                call.callerZone?.zoneName ?? "-"
            );

            // PRIORITY COLUMN
            const priorityCell =
                document.createElement("td");

            const priorityBadge =
                document.createElement("span");

            const priority = String(
                call.priority || "NORMAL"
            ).trim().toUpperCase();

            priorityBadge.textContent = priority;

            priorityBadge.className =
                "priority-badge " +
                priority.toLowerCase();

            priorityCell.appendChild(priorityBadge);
            row.appendChild(priorityCell);

            // AMBULANCE COLUMN
            addCell(
                row,
                call.ambulance?.ambulanceNumber ?? "Waiting"
            );

            // DISTANCE COLUMN
            const distanceCell =
                document.createElement("td");

            if (call.status === "COMPLETED") {
                distanceCell.textContent = "Completed";
            } else {
                distanceCell.textContent =
                    getDisplayedDistance(
                        call,
                        distanceMap
                    );
            }

            row.appendChild(distanceCell);

            // STATUS COLUMN
            const statusCell =
                document.createElement("td");

            const statusBadge =
                document.createElement("span");

            statusBadge.textContent = call.status;

            statusBadge.className =
                "call-status " +
                String(call.status).toLowerCase();

            statusCell.appendChild(statusBadge);
            row.appendChild(statusCell);

            // ACTION COLUMN
            const actionCell =
                document.createElement("td");

            const button =
                document.createElement("button");

            if (call.status === "PENDING") {

                button.textContent =
                    "Waiting for Ambulance";

                button.disabled = true;

            } else if (call.status === "ASSIGNED") {

                button.textContent = "Mark Arrived";
                button.className =
                    "action-button arrived";

                button.addEventListener("click", () => {
                    updateEmergencyCall(
                        call.id,
                        "arrived",
                        button
                    );
                });

            } else if (call.status === "ARRIVED") {

                button.textContent = "Complete";
                button.className =
                    "action-button complete";

                button.addEventListener("click", () => {
                    updateEmergencyCall(
                        call.id,
                        "complete",
                        button
                    );
                });

            } else {

                button.textContent = "Completed";
                button.disabled = true;
            }

            actionCell.appendChild(button);
            row.appendChild(actionCell);

            table.appendChild(row);
        });

        setMessage("callMessage", "");

    } catch (error) {
        console.error(error);

        setMessage(
            "callMessage",
            "Emergency calls load ஆகவில்லை: " +
            error.message,
            "error"
        );
    }
}

// Assignment is automatic.
// Only arrival and completion are manual.

async function updateEmergencyCall(
    callId,
    action,
    button
) {
    if (!["arrived", "complete"].includes(action)) {
        return;
    }

    button.disabled = true;

    setMessage(
        "callMessage",
        "Processing..."
    );

    try {
        const response = await fetch(
            `/emergency/${callId}/${action}`,
            {
                method: "PUT"
            }
        );

        if (!response.ok) {
            throw new Error(
                await getErrorMessage(response)
            );
        }

        await loadEmergencyCalls();

        setMessage(
            "callMessage",
            `Call #${callId} updated successfully!`,
            "success"
        );

    } catch (error) {
        console.error(error);

        setMessage(
            "callMessage",
            error.message,
            "error"
        );

        button.disabled = false;
    }
}

document.getElementById("refreshCalls")
    ?.addEventListener(
        "click",
        loadEmergencyCalls
    );


// ==================== ZONE MANAGEMENT ====================

async function loadZoneTable() {
    const table = document.getElementById(
        "zoneTable"
    );

    if (!table) return;

    try {
        const zones = await getData("/zone");

        table.replaceChildren();

        if (zones.length === 0) {
            addTableRow(table, [
                "-",
                "No zones registered yet"
            ]);
            return;
        }

        zones.forEach(zone => {
            addTableRow(table, [
                zone.id,
                zone.zoneName
            ]);
        });

    } catch (error) {
        setMessage(
            "zoneMessage",
            "Unable to load zones.",
            "error"
        );
    }
}

document.getElementById("zoneForm")
    ?.addEventListener("submit", async event => {
        event.preventDefault();

        const zoneName = document.getElementById(
            "zoneName"
        ).value.trim();

        const button = document.getElementById(
            "saveZone"
        );

        if (!zoneName) {
            setMessage(
                "zoneMessage",
                "Please enter a zone name.",
                "error"
            );
            return;
        }

        button.disabled = true;
        setMessage("zoneMessage", "Saving...");

        try {
            const response = await fetch("/zone", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    zoneName
                })
            });

            if (!response.ok) {
                throw new Error(
                    await getErrorMessage(response)
                );
            }

            event.target.reset();

            await loadZoneTable();

            setMessage(
                "zoneMessage",
                "Zone added successfully!",
                "success"
            );

        } catch (error) {
            setMessage(
                "zoneMessage",
                error.message,
                "error"
            );

        } finally {
            button.disabled = false;
        }
    });

document.getElementById("refreshZones")
    ?.addEventListener(
        "click",
        loadZoneTable
    );


// ==================== ZONE DISTANCES ====================

async function loadDistancePage() {
    await loadDistanceZones();
    await loadDistances();
}

async function loadDistanceZones() {
    const fromSelect = document.getElementById(
        "fromZone"
    );

    const toSelect = document.getElementById(
        "toZone"
    );

    if (!fromSelect || !toSelect) return;

    try {
        const zones = await getData("/zone");

        fromSelect.replaceChildren();
        toSelect.replaceChildren();

        const fromPlaceholder =
            document.createElement("option");

        fromPlaceholder.value = "";
        fromPlaceholder.textContent = "Select From Zone";

        fromSelect.appendChild(fromPlaceholder);

        const toPlaceholder =
            document.createElement("option");

        toPlaceholder.value = "";
        toPlaceholder.textContent = "Select To Zone";

        toSelect.appendChild(toPlaceholder);

        zones.forEach(zone => {
            const fromOption =
                document.createElement("option");

            fromOption.value = zone.id;
            fromOption.textContent = zone.zoneName;

            fromSelect.appendChild(fromOption);

            const toOption =
                document.createElement("option");

            toOption.value = zone.id;
            toOption.textContent = zone.zoneName;

            toSelect.appendChild(toOption);
        });

    } catch (error) {
        setMessage(
            "distanceMessage",
            "Unable to load zones.",
            "error"
        );
    }
}

async function loadDistances() {
    const table = document.getElementById(
        "distanceTable"
    );

    if (!table) return;

    try {
        const distances = await getData("/distance");

        table.replaceChildren();

        if (distances.length === 0) {
            addTableRow(table, [
                "-",
                "-",
                "-",
                "No distances added"
            ]);
            return;
        }

        distances.forEach(distance => {
            addTableRow(table, [
                distance.id,
                distance.fromZone?.zoneName ?? "-",
                distance.toZone?.zoneName ?? "-",
                distance.distanceKm
            ]);
        });

    } catch (error) {
        setMessage(
            "distanceMessage",
            "Unable to load distances.",
            "error"
        );
    }
}

document.getElementById("distanceForm")
    ?.addEventListener("submit", async event => {
        event.preventDefault();

        const fromId = Number(
            document.getElementById("fromZone").value
        );

        const toId = Number(
            document.getElementById("toZone").value
        );

        const distanceKm = Number(
            document.getElementById("distanceKm").value
        );

        const button = document.getElementById(
            "saveDistance"
        );

        if (!fromId || !toId) {
            setMessage(
                "distanceMessage",
                "Please select both zones.",
                "error"
            );
            return;
        }

        if (fromId === toId) {
            setMessage(
                "distanceMessage",
                "From and To zones must be different.",
                "error"
            );
            return;
        }

        if (!Number.isInteger(distanceKm) ||
            distanceKm < 1) {

            setMessage(
                "distanceMessage",
                "Enter a whole number greater than zero.",
                "error"
            );
            return;
        }

        button.disabled = true;

        setMessage(
            "distanceMessage",
            "Saving..."
        );

        try {
            const response = await fetch("/distance", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    fromZone: {
                        id: fromId
                    },
                    toZone: {
                        id: toId
                    },
                    distanceKm
                })
            });

            if (!response.ok) {
                throw new Error(
                    await getErrorMessage(response)
                );
            }

            event.target.reset();

            await loadDistances();

            setMessage(
                "distanceMessage",
                "Zone distance saved successfully!",
                "success"
            );

        } catch (error) {
            setMessage(
                "distanceMessage",
                error.message,
                "error"
            );

        } finally {
            button.disabled = false;
        }
    });

document.getElementById("refreshDistances")
    ?.addEventListener(
        "click",
        loadDistances
    );


// ==================== REPORTS ====================

function initializeReportDates() {
    const startInput = document.getElementById(
        "startDate"
    );

    const endInput = document.getElementById(
        "endDate"
    );

    if (!startInput || !endInput) return;

    const now = new Date();

    const year = now.getFullYear();

    const month = String(
        now.getMonth() + 1
    ).padStart(2, "0");

    const day = String(
        now.getDate()
    ).padStart(2, "0");

    const today = `${year}-${month}-${day}`;

    if (!startInput.value) {
        startInput.value = today;
    }

    if (!endInput.value) {
        endInput.value = today;
    }
}

document.getElementById("reportForm")
    ?.addEventListener("submit", async event => {
        event.preventDefault();

        const startDate = document.getElementById(
            "startDate"
        ).value;

        const endDate = document.getElementById(
            "endDate"
        ).value;

        const button = document.getElementById(
            "generateReport"
        );

        const table = document.getElementById(
            "reportTable"
        );

        if (startDate > endDate) {
            setMessage(
                "reportMessage",
                "Start Date cannot be after End Date.",
                "error"
            );
            return;
        }

        button.disabled = true;
        table.replaceChildren();

        setMessage(
            "reportMessage",
            "Generating report..."
        );

        try {
            const params = new URLSearchParams({
                startDate: startDate + "T00:00:00",
                endDate: endDate + "T23:59:59"
            });

            const report = await getData(
                "/report/average-response?" +
                params.toString()
            );

            const entries = Object.entries(report);

            if (entries.length === 0) {
                addTableRow(table, [
                    "No data",
                    "No completed arrivals for this date range"
                ]);

                setMessage(
                    "reportMessage",
                    "No response time data found."
                );

                return;
            }

            entries.forEach(([zone, minutes]) => {
                addTableRow(table, [
                    zone,
                    Number(minutes).toFixed(2) +
                    " minutes"
                ]);
            });

            setMessage(
                "reportMessage",
                "Report generated successfully!",
                "success"
            );

        } catch (error) {
            setMessage(
                "reportMessage",
                error.message,
                "error"
            );

        } finally {
            button.disabled = false;
        }
    });


// ==================== SETTINGS ====================

async function loadSettings() {
    const message = document.getElementById(
        "settingsMessage"
    );

    try {
        const user = await getData("/auth/me");

        document.getElementById(
            "profileName"
        ).textContent = user.fullName ?? "-";

        document.getElementById(
            "profileEmail"
        ).textContent = user.email ?? "-";

        document.getElementById(
            "profileRole"
        ).textContent = user.role ?? "-";

        if (message) {
            message.textContent = "";
            message.className = "";
        }

    } catch (error) {
        if (message) {
            message.textContent =
                "Profile load failed: " +
                error.message;

            message.className = "error";
        }
    }
}


// ==================== CHANGE PASSWORD ====================

document.getElementById("changePasswordForm")
    ?.addEventListener("submit", async event => {
        event.preventDefault();

        const message = document.getElementById(
            "settingsMessage"
        );

        const button = document.getElementById(
            "changePasswordButton"
        );

        const currentPassword =
            document.getElementById(
                "currentPassword"
            ).value;

        const newPassword =
            document.getElementById(
                "newPassword"
            ).value;

        const confirmPassword =
            document.getElementById(
                "confirmPassword"
            ).value;

        if (newPassword !== confirmPassword) {
            setMessage(
                "settingsMessage",
                "New passwords do not match.",
                "error"
            );
            return;
        }

        if (newPassword.length < 8) {
            setMessage(
                "settingsMessage",
                "Password must contain at least 8 characters.",
                "error"
            );
            return;
        }

        button.disabled = true;

        setMessage(
            "settingsMessage",
            "Updating password..."
        );

        try {
            const response = await fetch(
                "/auth/change-password",
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        currentPassword,
                        newPassword
                    })
                }
            );

            if (!response.ok) {
                throw new Error(
                    await getErrorMessage(response)
                );
            }

            event.target.reset();

            setMessage(
                "settingsMessage",
                "Password updated successfully!",
                "success"
            );

        } catch (error) {
            setMessage(
                "settingsMessage",
                error.message,
                "error"
            );

        } finally {
            button.disabled = false;
        }
    });


// ==================== LOGOUT ====================

document.getElementById("settingsLogout")
    ?.addEventListener("click", async () => {
        try {
            const response = await fetch(
                "/auth/logout",
                {
                    method: "POST"
                }
            );

            if (!response.ok) {
                throw new Error("Logout failed");
            }

            window.location.replace("/login.html");

        } catch (error) {
            setMessage(
                "settingsMessage",
                error.message,
                "error"
            );
        }
    });


// ==================== INITIAL PAGE ====================

showPage("dashboard");