document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form[data-redirect]");

    if (!form) return;

    form.addEventListener("submit", (event) => {
        event.preventDefault();

        if (!form.reportValidity()) return;

        const redirect = form.dataset.redirect;
        const formData = new FormData(form);

        if (form.dataset.confirmPassword) {
            const password = formData.get("senha");
            const confirmation = formData.get(form.dataset.confirmPassword);

            if (password !== confirmation) {
                alert("As senhas não coincidem.");
                return;
            }

            const account = {
                nome: formData.get("nome"),
                email: String(formData.get("email")).trim().toLowerCase(),
                senha: password
            };

            localStorage.setItem("uninassauConta", JSON.stringify(account));
            alert("Cadastro realizado com sucesso! Faça login para acessar o hub.");
            window.location.href = redirect;
            return;
        }

        const savedAccount = localStorage.getItem("uninassauConta");

        if (savedAccount) {
            const account = JSON.parse(savedAccount);
            const email = String(formData.get("email")).trim().toLowerCase();
            const senha = formData.get("senha");

            if (email !== account.email || senha !== account.senha) {
                alert("E-mail ou senha incorretos.");
                return;
            }
        }

        // Demonstração: se ainda não houver cadastro salvo neste navegador,
        // os campos obrigatórios preenchidos permitem acessar o hub.
        sessionStorage.setItem("uninassauLogado", "true");
        window.location.href = redirect;
    });
});
