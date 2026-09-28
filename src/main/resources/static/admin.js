
(async function () {

    try {
        const response = await fetch("/auth/me");

        if (!response.ok) {
            window.location.replace("/login.html");
            return;
        }

        const user = await response.json();

        if (user.role !== "ADMIN") {
            window.location.replace("/staff.html");
            return;
        }

        const logoutButton =
            document.createElement("button");

        logoutButton.textContent = "Logout";

        logoutButton.style.cssText = `
            position: fixed;
            top: 15px;
            right: 15px;
            z-index: 9999;
            background: #dc2626;
            color: white;
            border: none;
            padding: 10px 18px;
            border-radius: 8px;
            cursor: pointer;
        `;

        logoutButton.onclick = async function () {

            await fetch("/auth/logout", {
                method: "POST"
            });

            window.location.replace("/login.html");
        };

        document.body.appendChild(logoutButton);

    } catch (error) {
        window.location.replace("/login.html");
    }

})();