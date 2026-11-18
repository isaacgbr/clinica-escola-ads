
document.addEventListener("DOMContentLoaded", () => {
    // Atualiza o ano no rodapé
    const currentYear = document.getElementById("current-year");

    if (currentYear) {
        currentYear.textContent = new Date().getFullYear();
    }

    // Botão de sair
    const logoutButton = document.getElementById("logout");

    if (logoutButton) {
        logoutButton.addEventListener("click", () => {
            const confirmLogout = confirm(
                "Deseja realmente sair do painel administrativo?"
            );

            if (confirmLogout) {
                window.location.href = "../login_cadastro/cadastro/login.html";
            }
        });
    }

    console.log("Dashboard administrativo carregado com sucesso!");
});
