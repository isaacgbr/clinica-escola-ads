document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("#appointment-form");
  const list = document.querySelector("#appointment-list");
  const accountList = document.querySelector("#account-appointments");
  const accountData = document.querySelector("#account-data");
  const storageKey = "clinicaEscolaAgendamentos";
  const readAppointments = () => {
    try { return JSON.parse(localStorage.getItem(storageKey)) || []; }
    catch { return []; }
  };
  const renderAppointments = (target, items) => {
    if (!target) return;
    if (!items.length) {
      target.innerHTML = '<div class="empty-state">Nenhuma solicitação registrada neste navegador.</div>';
      return;
    }
    target.innerHTML = '<div class="data-list">' + items.map(item =>
      `<div class="data-item"><span>${item.servico} • ${item.data}</span><strong>${item.turno}</strong></div><div class="data-item"><span>Solicitante</span><strong>${item.nome}</strong></div>`
    ).join("") + '</div>';
  };
  if (form) {
    const date = form.querySelector('[name="data"]');
    date.min = new Date().toISOString().split("T")[0];
    renderAppointments(list, readAppointments());
    form.addEventListener("submit", event => {
      event.preventDefault();
      const item = Object.fromEntries(new FormData(form).entries());
      const appointments = readAppointments();
      appointments.unshift(item);
      localStorage.setItem(storageKey, JSON.stringify(appointments));
      const feedback = document.querySelector("#appointment-feedback");
      feedback.hidden = false;
      feedback.textContent = "Solicitação registrada neste navegador. Para confirmar um atendimento real, entre em contato com a unidade.";
      form.reset();
      date.min = new Date().toISOString().split("T")[0];
      renderAppointments(list, appointments);
    });
  }
  if (accountList) renderAppointments(accountList, readAppointments());
  if (accountData) {
    let user = null;
    for (const key of ["usuarioLogado", "usuario", "user"]) {
      const value = localStorage.getItem(key);
      if (!value) continue;
      try { user = JSON.parse(value); if (user) break; }
      catch { user = { nome: value }; break; }
    }
    if (user) {
      accountData.innerHTML = `<div class="data-item"><span>Nome</span><strong>${user.nome || user.name || "Usuário"}</strong></div><div class="data-item"><span>E-mail</span><strong>${user.email || "Não informado"}</strong></div>`;
    }
  }
});