/* ============================================
   CARGURU — MAIN JAVASCRIPT
   Sprint 1: Auth, Utilizadores, Veículos CRUD
   Sprint 2: Listagem, Filtros, Reservas, Detalhe
============================================ */

/* =================== DADOS MOCK ===================
   Veículos de exemplo para popular a listagem
   (Sprint 2 — CAR-20)
=================================================== */
const MOCK_VEHICLES = [
  { id: 'v1', marca: 'BMW', modelo: 'Série 3 320d', ano: 2022, combustivel: 'Gasóleo', transmissao: 'Automático', consumo: 5.2, lotacao: 5, km: 48000, precoDia: 65, localizacao: 'Porto', avaliacao: 4.8, icon: '🚗', proprietario: 'Carlos M.' },
  { id: 'v2', marca: 'Tesla', modelo: 'Model 3 Long Range', ano: 2023, combustivel: 'Elétrico', transmissao: 'Automático', consumo: 14.5, lotacao: 5, km: 8000, precoDia: 80, localizacao: 'Lisboa', avaliacao: 4.9, icon: '🚙', proprietario: 'Ana S.' },
  { id: 'v3', marca: 'Volkswagen', modelo: 'Tiguan 2.0 TDI', ano: 2021, combustivel: 'Gasóleo', transmissao: 'DSG', consumo: 5.8, lotacao: 5, km: 62000, precoDia: 55, localizacao: 'Braga', avaliacao: 4.5, icon: '🚐', proprietario: 'Pedro L.' },
  { id: 'v4', marca: 'Toyota', modelo: 'Yaris Hybrid', ano: 2023, combustivel: 'Híbrido', transmissao: 'CVT', consumo: 3.8, lotacao: 5, km: 5000, precoDia: 40, localizacao: 'Coimbra', avaliacao: 4.7, icon: '🚕', proprietario: 'Maria F.' },
  { id: 'v5', marca: 'Renault', modelo: 'Clio TCe 100', ano: 2022, combustivel: 'Gasolina', transmissao: 'Manual', consumo: 5.6, lotacao: 5, km: 28000, precoDia: 35, localizacao: 'Porto', avaliacao: 4.3, icon: '🚗', proprietario: 'João R.' },
  { id: 'v6', marca: 'Audi', modelo: 'A4 35 TDI', ano: 2021, combustivel: 'Gasóleo', transmissao: 'Automático', consumo: 4.9, lotacao: 5, km: 55000, precoDia: 70, localizacao: 'Lisboa', avaliacao: 4.6, icon: '🏎️', proprietario: 'Sofia C.' },
  { id: 'v7', marca: 'Ford', modelo: 'Puma ST-Line', ano: 2022, combustivel: 'Gasolina', transmissao: 'Manual', consumo: 6.1, lotacao: 5, km: 33000, precoDia: 45, localizacao: 'Setúbal', avaliacao: 4.4, icon: '🚗', proprietario: 'Rui A.' },
  { id: 'v8', marca: 'Mercedes-Benz', modelo: 'Classe A 200d', ano: 2022, combustivel: 'Gasóleo', transmissao: 'Automático', consumo: 4.5, lotacao: 5, km: 41000, precoDia: 75, localizacao: 'Lisboa', avaliacao: 4.8, icon: '🏎️', proprietario: 'Inês M.' },
  { id: 'v9', marca: 'Peugeot', modelo: '308 1.5 BlueHDi', ano: 2023, combustivel: 'Gasóleo', transmissao: 'Manual', consumo: 4.7, lotacao: 5, km: 12000, precoDia: 42, localizacao: 'Faro', avaliacao: 4.2, icon: '🚗', proprietario: 'Tiago B.' },
];

/* =================== PAGE ROUTER =================== */
const router = {
  pages: {},
  register(id, el) { this.pages[id] = el; },
  go(id) {
    // Rotas protegidas — requer login
    const protectedPages = ['dashboard', 'vehicles', 'conta', 'reservas'];
    if (protectedPages.includes(id) && !getSession()) {
      showToast('Precisas de iniciar sessão primeiro.', 'error');
      id = 'auth';
    }
    Object.values(this.pages).forEach(p => p.classList.remove('active'));
    if (this.pages[id]) {
      this.pages[id].classList.add('active');
      window.scrollTo({ top: 0, behavior: 'instant' });
    }
    document.querySelectorAll('[data-nav]').forEach(link => {
      link.classList.toggle('active', link.dataset.nav === id);
    });
    // Atualizar conteúdo dinâmico ao navegar
    if (id === 'dashboard') renderDashboard();
    if (id === 'vehicles') renderVehicles();
    if (id === 'conta') renderConta();
    if (id === 'reservas') renderReservas();
    if (id === 'home') updateHeroForAuth();
  }
};

/* =================== SESSION =================== */
function getSession() {
  try { return JSON.parse(localStorage.getItem('carguru_session')); } catch { return null; }
}
function setSession(data) { localStorage.setItem('carguru_session', JSON.stringify(data)); }
function clearSession() { localStorage.removeItem('carguru_session'); }

function getUsers() {
  try { return JSON.parse(localStorage.getItem('carguru_users') || '[]'); } catch { return []; }
}
function saveUsers(users) { localStorage.setItem('carguru_users', JSON.stringify(users)); }

function getCurrentUser() {
  const session = getSession();
  if (!session) return null;
  return getUsers().find(u => u.email === session.email) || null;
}
function saveCurrentUser(updated) {
  const users = getUsers();
  const idx = users.findIndex(u => u.email === updated.email);
  if (idx >= 0) { users[idx] = updated; saveUsers(users); }
}

/* =================== TOAST =================== */
function showToast(message, type = 'success') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${type === 'success' ? '✅' : '❌'}</span>
    <span class="toast-msg">${message}</span>
    <button class="toast-close" onclick="this.closest('.toast').remove()">×</button>
  `;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = '0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

/* =================== NAVBAR =================== */
// CAR-17 — Logout / CAR-44 — Perfil dual
function updateNavbar() {
  const session = getSession();
  const navGuest = document.getElementById('navGuest');
  const navUser  = document.getElementById('navUser');
  const navAvatar = document.getElementById('navAvatar');
  const navUserName = document.getElementById('navUserName');

  if (session) {
    navGuest.classList.add('hidden');
    navUser.classList.remove('hidden');
    const initials = session.name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
    navAvatar.textContent = initials;
    navUserName.textContent = session.name.split(' ')[0];
  } else {
    navGuest.classList.remove('hidden');
    navUser.classList.add('hidden');
  }
}

function initNavbar() {
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    navbar.classList.toggle('scrolled', window.scrollY > 20);
  });

  // Logout — CAR-17
  document.getElementById('logoutBtn')?.addEventListener('click', () => {
    clearSession();
    updateNavbar();
    showToast('Sessão terminada. Até logo! 👋');
    router.go('home');
  });

  // Avatar clicável vai para dashboard
  document.getElementById('navAvatar')?.addEventListener('click', () => router.go('dashboard'));
}

function updateHeroForAuth() {
  const heroCta2 = document.getElementById('heroCta2');
  if (!heroCta2) return;
  if (getSession()) {
    heroCta2.textContent = 'Ir para o Dashboard →';
    heroCta2.dataset.nav = 'dashboard';
  } else {
    heroCta2.textContent = 'Criar Conta Grátis';
    heroCta2.dataset.nav = 'auth';
  }
}

/* =================== AUTH — CAR-6, CAR-11, CAR-23 =================== */
function initAuth() {
  // Tabs
  document.querySelectorAll('.auth-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      const target = tab.dataset.tab;
      ['loginForm', 'registerForm', 'forgotForm'].forEach(id => {
        document.getElementById(id)?.classList.add('hidden');
      });
      document.getElementById(target === 'login' ? 'loginForm' : 'registerForm')?.classList.remove('hidden');
    });
  });

  // Toggle passwords
  document.querySelectorAll('.password-toggle').forEach(btn => {
    btn.addEventListener('click', () => {
      const input = btn.previousElementSibling;
      const isPass = input.type === 'password';
      input.type = isPass ? 'text' : 'password';
      btn.textContent = isPass ? '🙈' : '👁️';
    });
  });

  // Força da password — CAR-6
  document.getElementById('regPassword')?.addEventListener('input', function () {
    const val = this.value;
    const fill = document.getElementById('strengthFill');
    const text = document.getElementById('strengthText');
    let strength = 'weak', label = 'Fraca';
    if (val.length > 5) { strength = 'fair'; label = 'Razoável'; }
    if (val.length > 7 && /[A-Z]/.test(val)) { strength = 'good'; label = 'Boa'; }
    if (val.length > 9 && /[A-Z]/.test(val) && /[0-9]/.test(val) && /[^a-zA-Z0-9]/.test(val)) { strength = 'strong'; label = 'Forte 💪'; }
    fill.className = `strength-fill ${strength}`;
    text.textContent = val.length ? `Segurança: ${label}` : '';
  });

  // Recuperar password — CAR-23
  document.getElementById('forgotLink')?.addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('loginForm').classList.add('hidden');
    document.getElementById('forgotForm').classList.remove('hidden');
  });
  document.getElementById('backToLogin')?.addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('forgotForm').classList.add('hidden');
    document.getElementById('loginForm').classList.remove('hidden');
  });
  document.getElementById('forgotSubmit')?.addEventListener('click', () => {
    const email = document.getElementById('forgotEmail').value.trim();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showToast('Insere um email válido.', 'error'); return;
    }
    showToast('Se o email estiver registado, receberás instruções em breve. 📧');
    document.getElementById('forgotEmail').value = '';
    document.getElementById('forgotForm').classList.add('hidden');
    document.getElementById('loginForm').classList.remove('hidden');
  });

  document.getElementById('loginSubmit')?.addEventListener('click', handleLogin);
  document.getElementById('registerSubmit')?.addEventListener('click', handleRegister);
}

// CAR-11 — Login
function handleLogin() {
  const email    = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  if (!email || !password) { showToast('Preenche todos os campos.', 'error'); return; }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { showToast('Email inválido.', 'error'); return; }

  const user = getUsers().find(u => u.email === email && u.password === btoa(password));
  if (user) {
    setSession({ name: user.name, email: user.email });
    updateNavbar();
    showToast(`Bem-vindo de volta, ${user.name}! 🚗`);
    setTimeout(() => router.go('dashboard'), 1000);
  } else {
    showToast('Email ou password incorretos.', 'error');
  }
}

// CAR-6 — Registo
function handleRegister() {
  const name     = document.getElementById('regName').value.trim();
  const email    = document.getElementById('regEmail').value.trim();
  const nif      = document.getElementById('regNif').value.trim();
  const password = document.getElementById('regPassword').value;
  const confirm  = document.getElementById('regConfirm').value;
  const terms    = document.getElementById('regTerms').checked;

  if (!name || !email || !password || !confirm) { showToast('Preenche todos os campos obrigatórios.', 'error'); return; }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { showToast('Email inválido.', 'error'); return; }
  if (nif && !/^\d{9}$/.test(nif)) { showToast('NIF deve ter 9 dígitos.', 'error'); return; }
  if (password.length < 6) { showToast('A password deve ter pelo menos 6 caracteres.', 'error'); return; }
  if (password !== confirm) { showToast('As passwords não coincidem.', 'error'); return; }
  if (!terms) { showToast('Aceita os termos para continuar.', 'error'); return; }

  const users = getUsers();
  if (users.find(u => u.email === email)) { showToast('Este email já está registado.', 'error'); return; }

  const newUser = {
    name, email, nif: nif || '',
    password: btoa(password),
    saldo: 0,
    veiculos: [],
    reservas: [],
    createdAt: new Date().toISOString()
  };
  users.push(newUser);
  saveUsers(users);
  setSession({ name, email });
  updateNavbar();
  showToast(`Conta criada com sucesso! Bem-vindo, ${name}! 🎉`);
  setTimeout(() => router.go('dashboard'), 1000);
}

/* =================== DASHBOARD =================== */
function renderDashboard() {
  const user = getCurrentUser();
  if (!user) return;

  document.getElementById('dashUserName').textContent = user.name.toUpperCase();
  document.getElementById('dashStatVeiculos').textContent = (user.veiculos || []).length;
  document.getElementById('dashStatSaldo').textContent = (user.saldo || 0).toFixed(2) + '€';

  const reservas = (user.reservas || []).filter(r => r.estado === 'confirmada' || r.estado === 'pendente');
  document.getElementById('dashStatReservas').textContent = reservas.length;
  document.getElementById('dashStatAvaliacao').textContent = '—';

  // Lista veículos no dashboard
  const vList = document.getElementById('dashVehiclesList');
  const veiculos = user.veiculos || [];
  if (veiculos.length === 0) {
    vList.innerHTML = `<div class="dash-empty"><div class="dash-empty-icon">🚗</div><p>Ainda não anunciaste nenhum veículo.</p></div>`;
  } else {
    vList.innerHTML = veiculos.map(v => `
      <div class="dash-vehicle-item">
        <div class="dash-vehicle-icon">🚗</div>
        <div class="dash-vehicle-info">
          <div class="dash-vehicle-name">${v.marca} ${v.modelo}</div>
          <div class="dash-vehicle-sub">${v.ano} • ${v.combustivel} • ${v.localizacao}</div>
        </div>
        <div class="dash-vehicle-price">${v.precoDia}€/dia</div>
      </div>
    `).join('');
  }

  // Lista reservas no dashboard
  const rList = document.getElementById('dashReservasList');
  const allReservas = user.reservas || [];
  if (allReservas.length === 0) {
    rList.innerHTML = `<div class="dash-empty"><div class="dash-empty-icon">📅</div><p>Ainda não tens reservas.</p></div>`;
  } else {
    rList.innerHTML = allReservas.slice(0, 4).map(r => `
      <div class="dash-reserva-item">
        <span class="dash-reserva-badge ${badgeClass(r.estado)}">${r.estado.toUpperCase()}</span>
        <div class="dash-reserva-info">
          <div class="dash-reserva-name">${r.veiculo}</div>
          <div class="dash-reserva-sub">${r.dataInicio} → ${r.dataFim} • ${r.total}€</div>
        </div>
      </div>
    `).join('');
  }
}

function badgeClass(estado) {
  const map = { pendente: 'badge-pending', confirmada: 'badge-confirmed', cancelada: 'badge-cancelled', concluida: 'badge-completed' };
  return map[estado] || 'badge-pending';
}

/* =================== VEÍCULOS — CAR-20, CAR-22, CAR-24 =================== */
let currentVehicleFilters = {};
let selectedVehicle = null;

function getAllVehicles() {
  // Veículos mock + veículos dos utilizadores registados
  const users = getUsers();
  const userVehicles = [];
  users.forEach(u => {
    (u.veiculos || []).forEach(v => {
      userVehicles.push({ ...v, proprietario: u.name, proprietarioEmail: u.email });
    });
  });
  return [...MOCK_VEHICLES, ...userVehicles];
}

function renderVehicles() {
  const marca        = document.getElementById('fMarca').value;
  const combustivel  = document.getElementById('fCombustivel').value;
  const transmissao  = document.getElementById('fTransmissao').value;
  const localizacao  = document.getElementById('fLocalizacao').value;
  const precoMax     = parseFloat(document.getElementById('fPrecoMax').value) || Infinity;
  const sort         = document.getElementById('vehiclesSort').value;

  let vehicles = getAllVehicles();

  // Filtros — CAR-22
  if (marca)        vehicles = vehicles.filter(v => v.marca === marca);
  if (combustivel)  vehicles = vehicles.filter(v => v.combustivel === combustivel);
  if (transmissao)  vehicles = vehicles.filter(v => v.transmissao === transmissao);
  if (localizacao)  vehicles = vehicles.filter(v => v.localizacao === localizacao);
  if (precoMax < Infinity) vehicles = vehicles.filter(v => v.precoDia <= precoMax);

  // Ordenação
  vehicles.sort((a, b) => {
    if (sort === 'preco-asc')  return a.precoDia - b.precoDia;
    if (sort === 'preco-desc') return b.precoDia - a.precoDia;
    if (sort === 'avaliacao')  return (b.avaliacao || 0) - (a.avaliacao || 0);
    if (sort === 'nome')       return `${a.marca} ${a.modelo}`.localeCompare(`${b.marca} ${b.modelo}`);
    return 0;
  });

  document.getElementById('vehiclesCount').textContent = vehicles.length;

  const grid = document.getElementById('vehiclesGrid');
  if (vehicles.length === 0) {
    grid.innerHTML = `
      <div class="vehicles-empty">
        <div class="vehicles-empty-icon">🔍</div>
        <h3>Nenhum veículo encontrado</h3>
        <p>Tenta ajustar os filtros de pesquisa.</p>
      </div>`;
    return;
  }

  grid.innerHTML = vehicles.map(v => `
    <div class="vehicle-card" data-vid="${v.id}" onclick="openVehicleModal('${v.id}')">
      <div class="vehicle-card-img">${v.icon || '🚗'}</div>
      <div class="vehicle-card-body">
        <div class="vehicle-card-name">${v.marca} ${v.modelo}</div>
        <div class="vehicle-card-sub">${v.ano} • ${v.transmissao} • ${v.localizacao}</div>
        <div class="vehicle-card-specs">
          <span class="vehicle-spec">⛽ <strong>${v.consumo}</strong>${v.combustivel === 'Elétrico' ? 'kWh' : 'L'}/100km</span>
          <span class="vehicle-spec">👥 <strong>${v.lotacao}</strong> lug.</span>
          <span class="vehicle-spec">📏 <strong>${v.km.toLocaleString('pt')}</strong> km</span>
        </div>
        <div class="vehicle-card-footer">
          <div>
            <div class="vehicle-price-day">${v.precoDia}€</div>
            <div class="vehicle-price-label">por dia</div>
          </div>
          <div class="vehicle-rating">⭐ <strong>${v.avaliacao ? v.avaliacao.toFixed(1) : '—'}</strong></div>
        </div>
      </div>
    </div>
  `).join('');
}

function initVehiclesPage() {
  ['fMarca','fCombustivel','fTransmissao','fLocalizacao','fPrecoMax','vehiclesSort'].forEach(id => {
    document.getElementById(id)?.addEventListener('change', renderVehicles);
    document.getElementById(id)?.addEventListener('input', renderVehicles);
  });
  document.getElementById('filterReset')?.addEventListener('click', () => {
    ['fMarca','fCombustivel','fTransmissao','fLocalizacao'].forEach(id => { document.getElementById(id).value = ''; });
    document.getElementById('fPrecoMax').value = '';
    renderVehicles();
  });
}

/* =================== MODAL DETALHE — CAR-26 =================== */
function openVehicleModal(vid) {
  const vehicles = getAllVehicles();
  const v = vehicles.find(x => x.id === vid);
  if (!v) return;
  selectedVehicle = v;

  document.getElementById('modalVehicleName').textContent = `${v.marca} ${v.modelo}`;
  document.getElementById('modalVehicleSub').textContent  = `${v.ano} • ${v.combustivel} • ${v.localizacao}`;
  document.getElementById('modalCarIcon').textContent     = v.icon || '🚗';
  document.getElementById('modalPrice').textContent       = `${v.precoDia}€`;

  document.getElementById('modalSpecsGrid').innerHTML = [
    ['Combustível', v.combustivel],
    ['Transmissão', v.transmissao],
    ['Consumo',     `${v.consumo}${v.combustivel === 'Elétrico' ? 'kWh' : 'L'}/100km`],
    ['Lotação',     `${v.lotacao} lugares`],
    ['Quilómetros', `${v.km.toLocaleString('pt')} km`],
    ['Avaliação',   v.avaliacao ? `⭐ ${v.avaliacao.toFixed(1)}` : '—'],
  ].map(([label, val]) => `
    <div class="modal-spec-item">
      <div class="modal-spec-label">${label}</div>
      <div class="modal-spec-val">${val}</div>
    </div>
  `).join('');

  const initials = v.proprietario.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  document.getElementById('modalOwner').innerHTML = `
    <div class="modal-owner-avatar">${initials}</div>
    <div>
      <div class="modal-owner-name">${v.proprietario}</div>
      <div class="modal-owner-sub">Proprietário verificado ✔</div>
    </div>
  `;

  document.getElementById('vehicleModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeVehicleModal() {
  document.getElementById('vehicleModal').classList.add('hidden');
  document.body.style.overflow = '';
}

/* =================== MODAL RESERVA — CAR-8 =================== */
function openReservaModal(v) {
  selectedVehicle = v;
  document.getElementById('reservaModalSub').textContent = `${v.marca} ${v.modelo} — ${v.precoDia}€/dia`;
  document.getElementById('reservaDataInicio').value = '';
  document.getElementById('reservaDataFim').value = '';
  updateReservaSummary();

  closeVehicleModal();
  document.getElementById('reservaModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';

  // Datas mínimas = hoje
  const today = new Date().toISOString().split('T')[0];
  document.getElementById('reservaDataInicio').min = today;
  document.getElementById('reservaDataFim').min = today;
}

function updateReservaSummary() {
  if (!selectedVehicle) return;
  const inicio = document.getElementById('reservaDataInicio').value;
  const fim    = document.getElementById('reservaDataFim').value;

  if (!inicio || !fim || fim <= inicio) {
    document.getElementById('rsDias').textContent = '—';
    document.getElementById('rsPrecoDia').textContent = '—';
    document.getElementById('rsTotal').textContent = '—';
    return;
  }

  const dias = Math.ceil((new Date(fim) - new Date(inicio)) / 86400000);
  // Algoritmo de preço dinâmico — CAR-20 (preço base, sem fins de semana aqui para simplificar)
  let precoDia = selectedVehicle.precoDia;
  if (dias >= 7) precoDia = Math.round(precoDia * 0.9); // -10%

  const total = dias * precoDia;

  document.getElementById('rsDias').textContent   = `${dias} dia${dias !== 1 ? 's' : ''}`;
  document.getElementById('rsPrecoDia').textContent = `${precoDia}€`;
  document.getElementById('rsTotal').textContent  = `${total}€`;
}

function confirmarReserva() {
  if (!selectedVehicle) return;
  const user = getCurrentUser();
  if (!user) { showToast('Tens de iniciar sessão.', 'error'); return; }

  const inicio = document.getElementById('reservaDataInicio').value;
  const fim    = document.getElementById('reservaDataFim').value;

  if (!inicio || !fim) { showToast('Seleciona as datas de início e fim.', 'error'); return; }
  if (fim <= inicio)   { showToast('A data de fim deve ser posterior à de início.', 'error'); return; }

  const dias    = Math.ceil((new Date(fim) - new Date(inicio)) / 86400000);
  let precoDia  = selectedVehicle.precoDia;
  if (dias >= 7) precoDia = Math.round(precoDia * 0.9);
  const total   = dias * precoDia;

  // Validação de saldo — CAR-8
  const caucao = Math.ceil(total * 0.2);
  if ((user.saldo || 0) < caucao) {
    showToast(`Saldo insuficiente para a caução (${caucao}€). Deposita fundos na tua conta.`, 'error');
    return;
  }

  // Guardar reserva
  const reserva = {
    id: 'r' + Date.now(),
    veiculo: `${selectedVehicle.marca} ${selectedVehicle.modelo}`,
    veiculoId: selectedVehicle.id,
    dataInicio: inicio,
    dataFim: fim,
    dias,
    precoDia,
    total,
    caucao,
    estado: 'pendente',
    criadaEm: new Date().toISOString()
  };

  user.reservas = user.reservas || [];
  // Validação de sobreposição — CAR-12
  const sobreposicao = user.reservas.some(r =>
    r.veiculoId === selectedVehicle.id &&
    r.estado !== 'cancelada' &&
    r.dataInicio < fim && r.dataFim > inicio
  );
  if (sobreposicao) {
    showToast('Já tens uma reserva para este veículo nesse período.', 'error');
    return;
  }

  // Bloquear caução
  user.saldo = (user.saldo || 0) - caucao;
  user.reservas.push(reserva);
  saveCurrentUser(user);

  document.getElementById('reservaModal').classList.add('hidden');
  document.body.style.overflow = '';
  showToast(`Reserva submetida com sucesso! Caução de ${caucao}€ reservada. O proprietário tem 24h para aceitar. 🎉`);
  selectedVehicle = null;
}

/* =================== CONTA — CAR-28, CAR-9, CAR-13, CAR-15 =================== */
function renderConta() {
  const user = getCurrentUser();
  if (!user) return;

  const initials = user.name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  document.getElementById('contaAvatar').textContent = initials;
  document.getElementById('contaNome').textContent   = user.name;
  document.getElementById('contaEmail').textContent  = user.email;
  document.getElementById('contaSaldo').textContent  = (user.saldo || 0).toFixed(2);

  document.getElementById('perfilNome').value  = user.name;
  document.getElementById('perfilEmail').value = user.email;
  document.getElementById('perfilNif').value   = user.nif || '';

  renderContaVehicles();
}

function renderContaVehicles() {
  const user = getCurrentUser();
  if (!user) return;
  const veiculos = user.veiculos || [];
  const container = document.getElementById('contaVehiclesList');

  if (veiculos.length === 0) {
    container.innerHTML = `<div class="dash-empty"><div class="dash-empty-icon">🚗</div><p>Ainda não anunciaste nenhum veículo. Clica em <strong>+ Adicionar</strong> para começar.</p></div>`;
    return;
  }

  container.innerHTML = veiculos.map(v => `
    <div class="dash-vehicle-item">
      <div class="dash-vehicle-icon">🚗</div>
      <div class="dash-vehicle-info">
        <div class="dash-vehicle-name">${v.marca} ${v.modelo} (${v.ano})</div>
        <div class="dash-vehicle-sub">${v.combustivel} • ${v.transmissao} • ${v.localizacao} • ${v.km.toLocaleString('pt')} km</div>
      </div>
      <div style="display:flex;gap:8px;align-items:center;">
        <div class="dash-vehicle-price">${v.precoDia}€/dia</div>
        <button class="btn btn-outline btn-sm" onclick="openEditVehicle('${v.id}')">✏️</button>
        <button class="reserva-cancel-btn" onclick="removeVehicle('${v.id}')">🗑️</button>
      </div>
    </div>
  `).join('');
}

// CAR-9 — Adicionar veículo
function openAddVehicle() {
  document.getElementById('addVehicleModalTitle').textContent = 'Adicionar Veículo';
  ['vMarca','vModelo','vAno','vMatricula','vCombustivel','vTransmissao','vConsumo','vLotacao','vKm','vPrecoDia','vLocalizacao'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  document.getElementById('vEditId').value = '';
  document.getElementById('addVehicleModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

// CAR-13 — Editar veículo
function openEditVehicle(vid) {
  const user = getCurrentUser();
  const v = (user.veiculos || []).find(x => x.id === vid);
  if (!v) return;

  document.getElementById('addVehicleModalTitle').textContent = 'Editar Veículo';
  document.getElementById('vMarca').value       = v.marca;
  document.getElementById('vModelo').value      = v.modelo;
  document.getElementById('vAno').value         = v.ano;
  document.getElementById('vMatricula').value   = v.matricula || '';
  document.getElementById('vCombustivel').value = v.combustivel;
  document.getElementById('vTransmissao').value = v.transmissao;
  document.getElementById('vConsumo').value     = v.consumo;
  document.getElementById('vLotacao').value     = v.lotacao;
  document.getElementById('vKm').value          = v.km;
  document.getElementById('vPrecoDia').value    = v.precoDia;
  document.getElementById('vLocalizacao').value = v.localizacao;
  document.getElementById('vEditId').value      = vid;

  document.getElementById('addVehicleModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function saveVehicle() {
  const marca       = document.getElementById('vMarca').value;
  const modelo      = document.getElementById('vModelo').value.trim();
  const ano         = parseInt(document.getElementById('vAno').value);
  const matricula   = document.getElementById('vMatricula').value.trim();
  const combustivel = document.getElementById('vCombustivel').value;
  const transmissao = document.getElementById('vTransmissao').value;
  const consumo     = parseFloat(document.getElementById('vConsumo').value);
  const lotacao     = parseInt(document.getElementById('vLotacao').value);
  const km          = parseInt(document.getElementById('vKm').value);
  const precoDia    = parseFloat(document.getElementById('vPrecoDia').value);
  const localizacao = document.getElementById('vLocalizacao').value;
  const editId      = document.getElementById('vEditId').value;

  if (!marca || !modelo || !ano || !combustivel || !transmissao || !consumo || !lotacao || !km || !precoDia || !localizacao) {
    showToast('Preenche todos os campos obrigatórios.', 'error'); return;
  }
  if (matricula && !/^[0-9A-Z]{2}-[0-9A-Z]{2}-[0-9A-Z]{2}$/.test(matricula.toUpperCase())) {
    showToast('Formato de matrícula inválido (ex: 00-AA-00).', 'error'); return;
  }

  const user = getCurrentUser();
  user.veiculos = user.veiculos || [];

  if (editId) {
    // Editar — CAR-13
    const idx = user.veiculos.findIndex(v => v.id === editId);
    if (idx >= 0) {
      user.veiculos[idx] = { ...user.veiculos[idx], marca, modelo, ano, matricula: matricula.toUpperCase(), combustivel, transmissao, consumo, lotacao, km, precoDia, localizacao };
      showToast('Veículo atualizado com sucesso! ✅');
    }
  } else {
    // Adicionar — CAR-9
    user.veiculos.push({
      id: 'uv' + Date.now(), marca, modelo, ano,
      matricula: matricula.toUpperCase(),
      combustivel, transmissao, consumo, lotacao, km, precoDia, localizacao,
      icon: '🚗', avaliacao: null, criadoEm: new Date().toISOString()
    });
    showToast('Veículo adicionado com sucesso! 🚗');
  }

  saveCurrentUser(user);
  document.getElementById('addVehicleModal').classList.add('hidden');
  document.body.style.overflow = '';
  renderContaVehicles();
}

// CAR-15 — Remover veículo
function removeVehicle(vid) {
  if (!confirm('Tens a certeza que queres remover este veículo?')) return;
  const user = getCurrentUser();
  user.veiculos = (user.veiculos || []).filter(v => v.id !== vid);
  saveCurrentUser(user);
  renderContaVehicles();
  showToast('Veículo removido.', 'error');
}

function initConta() {
  document.getElementById('addVehicleBtn')?.addEventListener('click', openAddVehicle);
  document.getElementById('saveVehicleBtn')?.addEventListener('click', saveVehicle);
  document.getElementById('addVehicleModalClose')?.addEventListener('click', () => {
    document.getElementById('addVehicleModal').classList.add('hidden');
    document.body.style.overflow = '';
  });

  // Guardar perfil — CAR-28
  document.getElementById('perfilSaveBtn')?.addEventListener('click', () => {
    const user = getCurrentUser();
    const nome = document.getElementById('perfilNome').value.trim();
    const nif  = document.getElementById('perfilNif').value.trim();
    if (!nome) { showToast('O nome não pode estar vazio.', 'error'); return; }
    if (nif && !/^\d{9}$/.test(nif)) { showToast('NIF deve ter 9 dígitos.', 'error'); return; }
    user.name = nome;
    user.nif  = nif;
    saveCurrentUser(user);
    setSession({ name: nome, email: user.email });
    updateNavbar();
    showToast('Perfil atualizado com sucesso! ✅');
    renderConta();
  });

  // Saldo — depositar/levantar
  document.getElementById('depositarBtn')?.addEventListener('click', () => openSaldoModal('depositar'));
  document.getElementById('levantarBtn')?.addEventListener('click',  () => openSaldoModal('levantar'));
}

/* =================== SALDO — CAR-27, CAR-30 =================== */
let saldoAction = 'depositar';

function openSaldoModal(action) {
  saldoAction = action;
  const user = getCurrentUser();
  document.getElementById('saldoModalTitle').textContent  = action === 'depositar' ? 'Depositar Fundos' : 'Levantar Fundos';
  document.getElementById('saldoModalLabel').textContent  = action === 'depositar' ? 'Valor a depositar (€)' : 'Valor a levantar (€)';
  document.getElementById('saldoModalAtual').textContent  = (user.saldo || 0).toFixed(2) + '€';
  document.getElementById('saldoModalValor').value = '';
  document.getElementById('saldoModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function initSaldoModal() {
  document.getElementById('saldoModalClose')?.addEventListener('click', () => {
    document.getElementById('saldoModal').classList.add('hidden');
    document.body.style.overflow = '';
  });
  document.getElementById('saldoModalConfirm')?.addEventListener('click', () => {
    const valor = parseFloat(document.getElementById('saldoModalValor').value);
    if (!valor || valor <= 0) { showToast('Insere um valor válido.', 'error'); return; }
    const user = getCurrentUser();
    if (saldoAction === 'depositar') {
      user.saldo = (user.saldo || 0) + valor;
      showToast(`${valor.toFixed(2)}€ depositados com sucesso! 💶`);
    } else {
      if ((user.saldo || 0) < valor) { showToast('Saldo insuficiente.', 'error'); return; }
      user.saldo -= valor;
      showToast(`${valor.toFixed(2)}€ levantados com sucesso!`);
    }
    saveCurrentUser(user);
    document.getElementById('saldoModal').classList.add('hidden');
    document.body.style.overflow = '';
    document.getElementById('contaSaldo').textContent = (user.saldo || 0).toFixed(2);
  });
}

/* =================== RESERVAS — CAR-21 =================== */
function renderReservas() {
  const user = getCurrentUser();
  if (!user) return;
  const activeTab = document.querySelector('.reservas-tab.active')?.dataset.rtab || 'ativas';
  showReservasTab(activeTab);
}

function showReservasTab(tab) {
  const user = getCurrentUser();
  if (!user) return;
  const today = new Date().toISOString().split('T')[0];
  const all   = user.reservas || [];

  let filtered;
  if (tab === 'ativas')   filtered = all.filter(r => r.estado === 'confirmada' && r.dataFim >= today);
  if (tab === 'futuras')  filtered = all.filter(r => r.estado === 'pendente');
  if (tab === 'historico') filtered = all.filter(r => r.estado === 'cancelada' || r.estado === 'concluida' || r.dataFim < today);

  const container = document.getElementById('reservasList');
  if (!filtered || filtered.length === 0) {
    container.innerHTML = `
      <div class="reservas-empty">
        <div class="reservas-empty-icon">📅</div>
        <h3>Nenhuma reserva aqui</h3>
        <p>As tuas reservas aparecerão aqui após fazeres uma.</p>
      </div>`;
    return;
  }

  container.innerHTML = filtered.map(r => `
    <div class="reserva-item">
      <div class="reserva-item-icon">🚗</div>
      <div class="reserva-item-info">
        <div class="reserva-item-name">${r.veiculo}</div>
        <div class="reserva-item-sub"><span class="dash-reserva-badge ${badgeClass(r.estado)}">${r.estado.toUpperCase()}</span></div>
        <div class="reserva-item-dates" style="margin-top:6px;">
          <strong>${r.dataInicio}</strong> → <strong>${r.dataFim}</strong>
          (${r.dias} dia${r.dias !== 1 ? 's' : ''})
        </div>
      </div>
      <div class="reserva-item-right">
        <div class="reserva-item-price">${r.total}€</div>
        ${(r.estado === 'pendente' || r.estado === 'confirmada') ? `<button class="reserva-cancel-btn" onclick="cancelarReserva('${r.id}')">Cancelar</button>` : ''}
      </div>
    </div>
  `).join('');
}

// CAR-21 — Cancelamento de reserva
function cancelarReserva(rid) {
  if (!confirm('Tens a certeza que queres cancelar esta reserva?')) return;
  const user = getCurrentUser();
  const reserva = (user.reservas || []).find(r => r.id === rid);
  if (!reserva) return;

  const hoje = new Date();
  const inicio = new Date(reserva.dataInicio);
  const horas48 = 48 * 60 * 60 * 1000;
  const dentro48h = (inicio - hoje) < horas48;

  reserva.estado = 'cancelada';
  // Devolução da caução (parcial se < 48h)
  const devolucao = dentro48h ? Math.floor(reserva.caucao * 0.5) : reserva.caucao;
  user.saldo = (user.saldo || 0) + devolucao;

  saveCurrentUser(user);
  renderReservas();
  const msg = dentro48h
    ? `Reserva cancelada. Caução parcialmente reembolsada: ${devolucao}€ (50% por cancelamento tardio).`
    : `Reserva cancelada. Caução de ${devolucao}€ devolvida integralmente.`;
  showToast(msg);
}

function initReservasPage() {
  document.querySelectorAll('.reservas-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.reservas-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      showReservasTab(tab.dataset.rtab);
    });
  });
}

/* =================== MODALS FECHAR =================== */
function initModals() {
  document.getElementById('modalClose')?.addEventListener('click', closeVehicleModal);
  document.getElementById('vehicleModal')?.addEventListener('click', (e) => {
    if (e.target === e.currentTarget) closeVehicleModal();
  });

  document.getElementById('modalReserveBtn')?.addEventListener('click', () => {
    if (!getSession()) { showToast('Precisas de iniciar sessão para reservar.', 'error'); closeVehicleModal(); router.go('auth'); return; }
    openReservaModal(selectedVehicle);
  });

  document.getElementById('reservaModalClose')?.addEventListener('click', () => {
    document.getElementById('reservaModal').classList.add('hidden');
    document.body.style.overflow = '';
  });
  document.getElementById('reservaModal')?.addEventListener('click', (e) => {
    if (e.target === e.currentTarget) { document.getElementById('reservaModal').classList.add('hidden'); document.body.style.overflow = ''; }
  });
  ['reservaDataInicio','reservaDataFim'].forEach(id => {
    document.getElementById(id)?.addEventListener('change', updateReservaSummary);
  });
  document.getElementById('confirmarReservaBtn')?.addEventListener('click', confirmarReserva);

  document.getElementById('addVehicleModal')?.addEventListener('click', (e) => {
    if (e.target === e.currentTarget) { document.getElementById('addVehicleModal').classList.add('hidden'); document.body.style.overflow = ''; }
  });
  document.getElementById('saldoModal')?.addEventListener('click', (e) => {
    if (e.target === e.currentTarget) { document.getElementById('saldoModal').classList.add('hidden'); document.body.style.overflow = ''; }
  });
}

/* =================== SEARCH HOME =================== */
function initSearch() {
  document.getElementById('searchBtn')?.addEventListener('click', () => {
    const marca = document.getElementById('searchMarca').value;
    const tipo  = document.getElementById('searchTipo').value;
    const preco = document.getElementById('searchPreco').value;

    if (!getSession()) { router.go('auth'); return; }

    // Pré-preencher filtro de marca na página de veículos
    if (marca) document.getElementById('fMarca').value = marca;
    if (preco) document.getElementById('fPrecoMax').value = preco;
    router.go('vehicles');
  });
}

/* =================== SCROLL ANIMATIONS =================== */
function initScrollAnimations() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.style.opacity = '1';
        entry.target.style.transform = 'translateY(0)';
      }
    });
  }, { threshold: 0.1 });
  document.querySelectorAll('.feature-card, .car-card, .section-header').forEach(el => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(30px)';
    el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
    observer.observe(el);
  });
}

/* =================== MOBILE MENU =================== */
function initMobileMenu() {
  const hamburger = document.getElementById('hamburger');
  const navLinks  = document.getElementById('navLinks');
  if (hamburger && navLinks) {
    hamburger.addEventListener('click', () => {
      const open = navLinks.classList.toggle('mobile-open');
      hamburger.textContent = open ? '✕' : '☰';
    });
  }
}

/* =================== INIT =================== */
document.addEventListener('DOMContentLoaded', () => {
  // Registar páginas no router
  document.querySelectorAll('.page').forEach(page => router.register(page.id, page));

  // Links de navegação
  document.addEventListener('click', (e) => {
    const link = e.target.closest('[data-nav]');
    if (!link) return;
    // Não interferir com links dentro de modais ou formulários de outro propósito
    if (link.closest('.auth-form') && !link.classList.contains('nav-logo')) return;
    e.preventDefault();
    router.go(link.dataset.nav);
  });

  initNavbar();
  initAuth();
  initSearch();
  initVehiclesPage();
  initConta();
  initSaldoModal();
  initReservasPage();
  initModals();
  initScrollAnimations();
  initMobileMenu();

  updateNavbar();
  router.go('home');
});

/* ============================================================
   SPRINT 3 — Conta, Administração, Km, Avaliações, Combustível
   CAR-10 Aprovação proprietário  CAR-14 KM inicial
   CAR-16 Indisponibilidade       CAR-18 KM final + liquidação
   CAR-19 Validação admin         CAR-24 Filtro kms previstos
   CAR-25 Avaliação mútua         CAR-27 Saldo  CAR-30 Depósito
   CAR-31 Painel admin            CAR-32 Disputas
   CAR-35 Gestão caução           CAR-36 Bloqueio utilizadores
   CAR-37 Histórico transações    CAR-38 Combustível dinâmico
   CAR-41/43 Histórico alugueres  CAR-45 Admin utilizadores
============================================================ */

/* =================== ADMIN SESSION =================== */
// Conta admin hardcoded para demo (em prod seria role no user)
const ADMIN_EMAIL = 'admin@carguru.pt';
const ADMIN_PASS  = btoa('Admin123!');

function isAdmin() {
  const s = getSession();
  return s && s.email === ADMIN_EMAIL;
}

function ensureAdminExists() {
  const users = getUsers();
  if (!users.find(u => u.email === ADMIN_EMAIL)) {
    users.push({
      name: 'Administrador', email: ADMIN_EMAIL,
      password: ADMIN_PASS, nif: '999999999',
      saldo: 0, veiculos: [], reservas: [],
      isAdmin: true, createdAt: new Date().toISOString()
    });
    saveUsers(users);
  }
}

/* =================== COMBUSTÍVEL DINÂMICO — CAR-38 =================== */
const FUEL_DEFAULTS = {
  gasolina: { nome: 'Gasolina', icon: '⛽', base: 1.72 },
  gasoleo:  { nome: 'Gasóleo',  icon: '🛢️', base: 1.58 },
  gpl:      { nome: 'GPL',      icon: '🟢', base: 0.89 },
  eletrico: { nome: 'Elétrico', icon: '⚡', base: 0.21 },
};

function getFuelPrices() {
  try {
    return JSON.parse(localStorage.getItem('carguru_fuel') || 'null') ||
      Object.fromEntries(Object.entries(FUEL_DEFAULTS).map(([k, v]) => [k, { ...v, current: v.base }]));
  } catch { return Object.fromEntries(Object.entries(FUEL_DEFAULTS).map(([k, v]) => [k, { ...v, current: v.base }])); }
}
function saveFuelPrices(prices) { localStorage.setItem('carguru_fuel', JSON.stringify(prices)); }

function applyFuelVariation() {
  const prices = getFuelPrices();
  Object.keys(prices).forEach(k => {
    const variation = 1 + (Math.random() * 0.10 - 0.05); // ±5%
    prices[k].current = Math.round(prices[k].base * variation * 1000) / 1000;
  });
  saveFuelPrices(prices);
}

let fuelTimer = null;
let fuelCountdownVal = 600; // 10 min em segundos

function startFuelTimer() {
  if (fuelTimer) clearInterval(fuelTimer);
  fuelCountdownVal = 600;
  fuelTimer = setInterval(() => {
    fuelCountdownVal--;
    const el = document.getElementById('fuelCountdown');
    if (el) {
      const m = String(Math.floor(fuelCountdownVal / 60)).padStart(2, '0');
      const s = String(fuelCountdownVal % 60).padStart(2, '0');
      el.textContent = `${m}:${s}`;
    }
    if (fuelCountdownVal <= 0) {
      applyFuelVariation();
      fuelCountdownVal = 600;
      renderFuelPrices();
    }
  }, 1000);
}

function renderFuelPrices() {
  const grid = document.getElementById('fuelPriceGrid');
  if (!grid) return;
  const prices = getFuelPrices();
  grid.innerHTML = Object.entries(prices).map(([k, v]) => `
    <div class="fuel-price-card">
      <div class="fuel-price-left">
        <span class="fuel-icon">${v.icon}</span>
        <div>
          <div class="fuel-name">${v.nome}</div>
          <div class="fuel-current">Atual: <strong>${v.current.toFixed(3)}€/L</strong></div>
        </div>
      </div>
      <div class="fuel-input-wrap">
        <input type="number" class="fuel-input" id="fuelInput-${k}"
          value="${v.base.toFixed(3)}" step="0.001" min="0.1" />
        <button class="admin-action-btn btn-approve" onclick="saveFuelBase('${k}')">✔</button>
      </div>
    </div>
  `).join('');
}

function saveFuelBase(key) {
  const val = parseFloat(document.getElementById(`fuelInput-${key}`)?.value);
  if (!val || val <= 0) { showToast('Valor inválido.', 'error'); return; }
  const prices = getFuelPrices();
  prices[key].base = val;
  prices[key].current = val;
  saveFuelPrices(prices);
  renderFuelPrices();
  showToast(`Preço base do ${prices[key].nome} atualizado para ${val.toFixed(3)}€/L ✅`);
}

/* =================== CAR-24 — FILTRO KMS PREVISTOS =================== */
// Injeta input de kms na página de veículos e re-ordena por custo total estimado
function injectKmsFilter() {
  const toolbar = document.querySelector('.vehicles-sort');
  if (!toolbar || document.getElementById('fKmsPrevistos')) return;
  const wrap = document.createElement('div');
  wrap.style.cssText = 'display:flex;align-items:center;gap:8px;margin-left:12px;';
  wrap.innerHTML = `
    <label style="font-size:0.78rem;color:var(--grey-text);white-space:nowrap;">KMs previstos:</label>
    <input type="number" id="fKmsPrevistos" placeholder="200"
      style="width:80px;background:var(--black-card);border:1px solid var(--black-border);
             border-radius:var(--radius-sm);padding:7px 10px;color:var(--white);font-size:0.82rem;"
      min="0" step="50" />
  `;
  toolbar.appendChild(wrap);
  document.getElementById('fKmsPrevistos').addEventListener('input', renderVehicles);
}

// Patch renderVehicles to include kms filter — override sort
const _origRenderVehicles = renderVehicles;
// We extend it below by adding kms logic inside the sort block (already in renderVehicles via vehiclesSort)

/* =================== ADMIN PAGE — CAR-31 =================== */
function initAdminPage() {
  document.querySelectorAll('.admin-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      document.querySelectorAll('.admin-section').forEach(s => s.classList.remove('active'));
      const section = document.getElementById(`aSection-${tab.dataset.atab}`);
      if (section) section.classList.add('active');
      renderAdminSection(tab.dataset.atab);
    });
  });
  document.getElementById('adminHistFilter')?.addEventListener('click', () => renderAdminSection('historico-global'));
}

function renderAdminSection(section) {
  if (section === 'veiculos-pendentes') renderAdminVeiculos();
  if (section === 'utilizadores')       renderAdminUtilizadores();
  if (section === 'combustivel')        { renderFuelPrices(); startFuelTimer(); }
  if (section === 'disputas')           renderAdminDisputas();
  if (section === 'historico-global')   renderAdminHistorico();
}

// CAR-19 — Validação anúncios
function renderAdminVeiculos() {
  const tbody = document.getElementById('adminVeiculosTbody');
  if (!tbody) return;
  const users = getUsers().filter(u => !u.isAdmin);
  const rows = [];
  users.forEach(u => {
    (u.veiculos || []).forEach(v => {
      rows.push({ ...v, ownerName: u.name, ownerEmail: u.email });
    });
  });
  if (rows.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--grey-text);padding:32px;">Sem veículos para validar.</td></tr>`;
    return;
  }
  tbody.innerHTML = rows.map(v => `
    <tr>
      <td><strong>${v.marca} ${v.modelo}</strong> (${v.ano})</td>
      <td>${v.ownerName}</td>
      <td>${v.localizacao}</td>
      <td>${v.precoDia}€/dia</td>
      <td><span class="dash-reserva-badge ${v.validado === false ? 'badge-cancelled' : v.validado ? 'badge-confirmed' : 'badge-pending'}">${v.validado === false ? 'REJEITADO' : v.validado ? 'APROVADO' : 'PENDENTE'}</span></td>
      <td>
        <button class="admin-action-btn btn-approve" onclick="adminValidarVeiculo('${v.ownerEmail}','${v.id}',true)">✔ Aprovar</button>
        <button class="admin-action-btn btn-reject"  onclick="adminValidarVeiculo('${v.ownerEmail}','${v.id}',false)">✕ Rejeitar</button>
      </td>
    </tr>
  `).join('');
}

function adminValidarVeiculo(email, vid, aprovado) {
  const users = getUsers();
  const user = users.find(u => u.email === email);
  if (!user) return;
  const v = (user.veiculos || []).find(x => x.id === vid);
  if (v) { v.validado = aprovado; saveUsers(users); }
  renderAdminVeiculos();
  showToast(aprovado ? `Veículo aprovado ✅` : `Veículo rejeitado ❌`, aprovado ? 'success' : 'error');
}

// CAR-36, CAR-45 — Bloqueio utilizadores
function renderAdminUtilizadores() {
  const tbody = document.getElementById('adminUtilizadoresTbody');
  if (!tbody) return;
  const users = getUsers().filter(u => !u.isAdmin);
  if (users.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:var(--grey-text);padding:32px;">Sem utilizadores registados.</td></tr>`;
    return;
  }
  tbody.innerHTML = users.map(u => `
    <tr>
      <td><strong>${u.name}</strong></td>
      <td style="font-size:0.78rem;color:var(--grey-text);">${u.email}</td>
      <td>${u.nif || '—'}</td>
      <td>${(u.saldo || 0).toFixed(2)}€</td>
      <td>${(u.veiculos || []).length}</td>
      <td>${(u.reservas || []).length}</td>
      <td><span class="dash-reserva-badge ${u.bloqueado ? 'badge-cancelled' : 'badge-confirmed'}">${u.bloqueado ? 'BLOQUEADO' : 'ATIVO'}</span></td>
      <td>
        ${u.bloqueado
          ? `<button class="admin-action-btn btn-unblock" onclick="adminBloquearUser('${u.email}',false)">🔓 Desbloquear</button>`
          : `<button class="admin-action-btn btn-block"   onclick="adminBloquearUser('${u.email}',true)">🔒 Bloquear</button>`
        }
      </td>
    </tr>
  `).join('');
}

function adminBloquearUser(email, bloquear) {
  const users = getUsers();
  const u = users.find(x => x.email === email);
  if (u) { u.bloqueado = bloquear; saveUsers(users); }
  renderAdminUtilizadores();
  showToast(bloquear ? `Utilizador bloqueado 🔒` : `Utilizador desbloqueado 🔓`, bloquear ? 'error' : 'success');
}

// Verificar bloqueio no login
const _origHandleLogin = handleLogin;
// Patch login to check block
function handleLoginPatched() {
  const email    = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  if (!email || !password) { showToast('Preenche todos os campos.', 'error'); return; }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { showToast('Email inválido.', 'error'); return; }
  const user = getUsers().find(u => u.email === email && u.password === btoa(password));
  if (!user) { showToast('Email ou password incorretos.', 'error'); return; }
  if (user.bloqueado) { showToast('A tua conta foi bloqueada. Contacta o suporte.', 'error'); return; }
  setSession({ name: user.name, email: user.email, isAdmin: !!user.isAdmin });
  updateNavbar();
  showToast(`Bem-vindo de volta, ${user.name}! 🚗`);
  setTimeout(() => router.go(user.isAdmin ? 'admin' : 'dashboard'), 1000);
}

// CAR-32 — Disputas
function renderAdminDisputas() {
  const container = document.getElementById('adminDisputasList');
  if (!container) return;
  const disputas = getAllDisputas();
  if (disputas.length === 0) {
    container.innerHTML = `<div class="dash-empty"><div class="dash-empty-icon">⚖️</div><p>Sem disputas abertas de momento.</p></div>`;
    return;
  }
  container.innerHTML = disputas.map(d => `
    <div class="disputa-item">
      <div class="disputa-header">
        <div class="disputa-title">⚖️ ${d.titulo}</div>
        <span class="dash-reserva-badge ${d.resolvida ? 'badge-completed' : 'badge-pending'}">${d.resolvida ? 'RESOLVIDA' : 'ABERTA'}</span>
      </div>
      <div class="disputa-meta">${d.autor} • ${d.data}</div>
      <div class="disputa-desc">${d.descricao}</div>
      ${!d.resolvida ? `
        <div class="disputa-actions">
          <button class="admin-action-btn btn-approve" onclick="resolverDisputa('${d.id}','proprietario')">✔ Favor Proprietário</button>
          <button class="admin-action-btn btn-approve" onclick="resolverDisputa('${d.id}','locatario')">✔ Favor Locatário</button>
          <button class="admin-action-btn btn-reject"  onclick="resolverDisputa('${d.id}','improcedente')">✕ Improcedente</button>
        </div>` : `<p style="font-size:0.78rem;color:var(--grey-text);">Decisão: <strong style="color:var(--orange);">${d.decisao}</strong></p>`
      }
    </div>
  `).join('');
}

function getAllDisputas() {
  try { return JSON.parse(localStorage.getItem('carguru_disputas') || '[]'); } catch { return []; }
}
function saveDisputas(d) { localStorage.setItem('carguru_disputas', JSON.stringify(d)); }

function abrirDisputa(reservaId, descricao) {
  const user = getCurrentUser();
  if (!user || !descricao.trim()) return;
  const disputas = getAllDisputas();
  disputas.push({
    id: 'd' + Date.now(), titulo: `Disputa — Reserva ${reservaId}`,
    autor: user.name, data: new Date().toLocaleDateString('pt-PT'),
    descricao, resolvida: false, reservaId
  });
  saveDisputas(disputas);
  showToast('Disputa aberta. A administração irá analisar em breve. ⚖️');
}

function resolverDisputa(id, decisao) {
  const disputas = getAllDisputas();
  const d = disputas.find(x => x.id === id);
  if (d) { d.resolvida = true; d.decisao = decisao; saveDisputas(disputas); }
  renderAdminDisputas();
  showToast(`Disputa resolvida: ${decisao} ✅`);
}

// CAR-34 — Histórico global alugueres
function renderAdminHistorico() {
  const tbody = document.getElementById('adminHistTbody');
  if (!tbody) return;
  const inicio = document.getElementById('adminHistInicio')?.value || '';
  const fim    = document.getElementById('adminHistFim')?.value || '';
  const users  = getUsers().filter(u => !u.isAdmin);
  const all    = [];
  users.forEach(u => {
    (u.reservas || []).forEach(r => all.push({ ...r, locatario: u.name }));
  });
  const filtered = all.filter(r => {
    if (inicio && r.dataInicio < inicio) return false;
    if (fim    && r.dataFim   > fim)    return false;
    return true;
  });
  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--grey-text);padding:32px;">Sem registos para o período selecionado.</td></tr>`;
    return;
  }
  tbody.innerHTML = filtered.map(r => `
    <tr>
      <td style="font-size:0.72rem;color:var(--grey-text);">${r.id}</td>
      <td><strong>${r.veiculo}</strong></td>
      <td>${r.locatario}</td>
      <td>${r.dataInicio}</td>
      <td>${r.dataFim}</td>
      <td style="color:var(--orange);font-weight:700;">${r.total}€</td>
      <td><span class="dash-reserva-badge ${badgeClass(r.estado)}">${r.estado.toUpperCase()}</span></td>
    </tr>
  `).join('');
}

/* =================== CAR-10 — APROVAÇÃO PROPRIETÁRIO =================== */
// Adiciona painel de reservas recebidas na página Conta
function renderContaReservasRecebidas() {
  const user = getCurrentUser();
  if (!user) return;
  const container = document.getElementById('contaReservasRecebidasList');
  if (!container) return;
  // Encontrar reservas dos veículos do utilizador atual
  const meusVeiculoIds = (user.veiculos || []).map(v => v.id);
  const allUsers = getUsers();
  const reservasRecebidas = [];
  allUsers.forEach(u => {
    (u.reservas || []).forEach(r => {
      if (meusVeiculoIds.includes(r.veiculoId) && r.estado === 'pendente') {
        reservasRecebidas.push({ ...r, locatarioNome: u.name, locatarioEmail: u.email });
      }
    });
  });
  if (reservasRecebidas.length === 0) {
    container.innerHTML = `<div class="dash-empty"><div class="dash-empty-icon">📬</div><p>Sem pedidos de reserva pendentes.</p></div>`;
    return;
  }
  container.innerHTML = reservasRecebidas.map(r => `
    <div class="dash-vehicle-item">
      <div class="dash-vehicle-icon">📬</div>
      <div class="dash-vehicle-info">
        <div class="dash-vehicle-name">${r.veiculo}</div>
        <div class="dash-vehicle-sub">De: ${r.locatarioNome} • ${r.dataInicio} → ${r.dataFim} (${r.dias}d)</div>
      </div>
      <div style="display:flex;gap:8px;">
        <button class="admin-action-btn btn-approve" onclick="aceitarReserva('${r.locatarioEmail}','${r.id}')">✔ Aceitar</button>
        <button class="admin-action-btn btn-reject"  onclick="rejeitarReserva('${r.locatarioEmail}','${r.id}')">✕ Rejeitar</button>
      </div>
    </div>
  `).join('');
}

function aceitarReserva(locatarioEmail, rid) {
  const users = getUsers();
  const loc = users.find(u => u.email === locatarioEmail);
  if (!loc) return;
  const r = (loc.reservas || []).find(x => x.id === rid);
  if (r) { r.estado = 'confirmada'; saveUsers(users); }
  // Registar transação
  addTransacao(locatarioEmail, { tipo: 'saida', desc: `Reserva confirmada — ${r.veiculo}`, valor: r.caucao, data: new Date().toLocaleDateString('pt-PT') });
  renderContaReservasRecebidas();
  showToast('Reserva aceite! O locatário foi notificado. ✅');
}

function rejeitarReserva(locatarioEmail, rid) {
  const users = getUsers();
  const loc = users.find(u => u.email === locatarioEmail);
  if (!loc) return;
  const r = (loc.reservas || []).find(x => x.id === rid);
  if (r) {
    r.estado = 'cancelada';
    loc.saldo = (loc.saldo || 0) + r.caucao; // devolver caução
    saveUsers(users);
  }
  renderContaReservasRecebidas();
  showToast('Reserva rejeitada. Caução devolvida ao locatário.');
}

/* =================== CAR-35 — GESTÃO CAUÇÃO =================== */
// Já integrada no fluxo de reserva (20% bloqueado) e em aceitarReserva/cancelarReserva

/* =================== CAR-37 — HISTÓRICO TRANSAÇÕES =================== */
function getTransacoes(email) {
  try { return JSON.parse(localStorage.getItem(`carguru_trans_${email}`) || '[]'); } catch { return []; }
}
function addTransacao(email, t) {
  const ts = getTransacoes(email);
  ts.unshift({ ...t, id: 't' + Date.now() });
  localStorage.setItem(`carguru_trans_${email}`, JSON.stringify(ts.slice(0, 100))); // max 100
}

function renderTransacoes() {
  const user = getCurrentUser();
  const container = document.getElementById('contaTransacoesList');
  if (!container || !user) return;
  const ts = getTransacoes(user.email);
  if (ts.length === 0) {
    container.innerHTML = `<div class="dash-empty"><div class="dash-empty-icon">📋</div><p>Sem transações registadas.</p></div>`;
    return;
  }
  container.innerHTML = ts.map(t => `
    <div class="transacao-item">
      <div class="transacao-icon ${t.tipo}">${t.tipo === 'entrada' ? '⬆️' : '⬇️'}</div>
      <div class="transacao-info">
        <div class="transacao-desc">${t.desc}</div>
        <div class="transacao-date">${t.data}</div>
      </div>
      <div class="transacao-val ${t.tipo}">${t.tipo === 'entrada' ? '+' : '-'}${t.valor.toFixed(2)}€</div>
    </div>
  `).join('');
}

/* =================== CAR-14 — KM INICIAL =================== */
function openKmInicial(reservaId) {
  const user = getCurrentUser();
  const r = (user?.reservas || []).find(x => x.id === reservaId);
  if (!r) return;
  document.getElementById('kmInicialReservaInfo').value = `${r.veiculo} • ${r.dataInicio} → ${r.dataFim}`;
  document.getElementById('kmInicialValor').value = '';
  document.getElementById('kmInicialReservaId').value = reservaId;
  document.getElementById('kmInicialModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function confirmarKmInicial() {
  const km  = parseInt(document.getElementById('kmInicialValor').value);
  const rid = document.getElementById('kmInicialReservaId').value;
  if (!km || km < 0) { showToast('Insere uma quilometragem válida.', 'error'); return; }
  const user = getCurrentUser();
  const r = (user.reservas || []).find(x => x.id === rid);
  if (!r) return;
  r.kmInicial = km;
  r.estado = 'confirmada';
  saveCurrentUser(user);
  document.getElementById('kmInicialModal').classList.add('hidden');
  document.body.style.overflow = '';
  showToast(`KM inicial registado: ${km.toLocaleString('pt')} km ✅`);
  renderReservas();
}

/* =================== CAR-18 — KM FINAL + LIQUIDAÇÃO =================== */
function openKmFinal(reservaId) {
  const user = getCurrentUser();
  const r = (user?.reservas || []).find(x => x.id === reservaId);
  if (!r) return;
  document.getElementById('kmFinalInicialInfo').value = r.kmInicial ? `${r.kmInicial.toLocaleString('pt')} km` : 'Não registado';
  document.getElementById('kmFinalValor').value = '';
  document.getElementById('kmFinalReservaId').value = reservaId;
  document.getElementById('kmFinalSummary').style.display = 'none';
  document.getElementById('kmFinalModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('kmFinalValor')?.addEventListener('input', calcKmFinalPreview);
});

function calcKmFinalPreview() {
  const kmFinal = parseInt(document.getElementById('kmFinalValor').value);
  const rid = document.getElementById('kmFinalReservaId').value;
  if (!rid) return;
  const user = getCurrentUser();
  const r = (user?.reservas || []).find(x => x.id === rid);
  if (!r || !kmFinal || kmFinal <= (r.kmInicial || 0)) {
    document.getElementById('kmFinalSummary').style.display = 'none'; return;
  }
  const kms = kmFinal - (r.kmInicial || 0);
  // Preço combustível atual
  const prices = getFuelPrices();
  const fuelKey = { 'Gasolina': 'gasolina', 'Gasóleo': 'gasoleo', 'Elétrico': 'eletrico', 'GPL': 'gpl', 'Híbrido': 'gasolina' };
  const veiculo = getAllVehicles().find(v => v.id === r.veiculoId) || {};
  const fuelPrice = prices[fuelKey[veiculo.combustivel] || 'gasolina']?.current || 1.7;
  const consumo = veiculo.consumo || 6;
  const custoCombustivel = Math.round((kms / 100) * consumo * fuelPrice * 100) / 100;
  const renda = r.total;
  const totalFinal = renda + custoCombustivel;

  document.getElementById('kfKms').textContent = `${kms.toLocaleString('pt')} km`;
  document.getElementById('kfRenda').textContent = `${renda}€`;
  document.getElementById('kfCombustivel').textContent = `${custoCombustivel.toFixed(2)}€`;
  document.getElementById('kfCaucao').textContent = `+${r.caucao}€ devolvida`;
  document.getElementById('kfTotal').textContent = `${totalFinal.toFixed(2)}€`;
  document.getElementById('kmFinalSummary').style.display = 'block';
}

function confirmarKmFinal() {
  const kmFinal = parseInt(document.getElementById('kmFinalValor').value);
  const rid = document.getElementById('kmFinalReservaId').value;
  const user = getCurrentUser();
  const r = (user?.reservas || []).find(x => x.id === rid);
  if (!r) return;
  if (!kmFinal || kmFinal <= (r.kmInicial || 0)) { showToast('KM final deve ser superior ao inicial.', 'error'); return; }

  const kms = kmFinal - r.kmInicial;
  const prices = getFuelPrices();
  const fuelKey = { 'Gasolina': 'gasolina', 'Gasóleo': 'gasoleo', 'Elétrico': 'eletrico', 'GPL': 'gpl', 'Híbrido': 'gasolina' };
  const veiculo = getAllVehicles().find(v => v.id === r.veiculoId) || {};
  const fuelPrice = prices[fuelKey[veiculo.combustivel] || 'gasolina']?.current || 1.7;
  const custoCombustivel = Math.round((kms / 100) * (veiculo.consumo || 6) * fuelPrice * 100) / 100;

  r.kmFinal = kmFinal;
  r.kmsPercorridos = kms;
  r.custoCombustivel = custoCombustivel;
  r.totalFinal = r.total + custoCombustivel;
  r.estado = 'concluida';
  // Devolver caução (caução - custo combustível excedente se houver dano seria 0, aqui assume-se sem incidentes)
  user.saldo = (user.saldo || 0) + r.caucao;

  addTransacao(user.email, {
    tipo: 'saida', desc: `Liquidação — ${r.veiculo} (${kms}km)`,
    valor: r.totalFinal, data: new Date().toLocaleDateString('pt-PT')
  });
  addTransacao(user.email, {
    tipo: 'entrada', desc: `Caução devolvida — ${r.veiculo}`,
    valor: r.caucao, data: new Date().toLocaleDateString('pt-PT')
  });

  saveCurrentUser(user);
  document.getElementById('kmFinalModal').classList.add('hidden');
  document.body.style.overflow = '';
  showToast(`Aluguer concluído! Total final: ${r.totalFinal.toFixed(2)}€ 🏁`);

  // Abrir modal de avaliação automaticamente
  setTimeout(() => openAvaliacaoModal(rid), 1000);
  renderReservas();
}

/* =================== CAR-25 — AVALIAÇÃO MÚTUA =================== */
let starSelected = 0;

function openAvaliacaoModal(reservaId) {
  const user = getCurrentUser();
  const r = (user?.reservas || []).find(x => x.id === reservaId);
  if (!r) return;
  document.getElementById('avaliacaoDesc').textContent = `Como foi a tua experiência a alugar o ${r.veiculo}?`;
  document.getElementById('avaliacaoReservaId').value = reservaId;
  document.getElementById('avaliacaoComentario').value = '';
  starSelected = 0;
  document.querySelectorAll('.star-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('avaliacaoModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function initAvaliacaoModal() {
  document.querySelectorAll('.star-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      starSelected = parseInt(btn.dataset.star);
      document.querySelectorAll('.star-btn').forEach(b => {
        b.classList.toggle('active', parseInt(b.dataset.star) <= starSelected);
      });
    });
  });
  document.getElementById('avaliacaoClose')?.addEventListener('click', () => {
    document.getElementById('avaliacaoModal').classList.add('hidden');
    document.body.style.overflow = '';
  });
  document.getElementById('avaliacaoConfirm')?.addEventListener('click', () => {
    if (!starSelected) { showToast('Seleciona uma classificação.', 'error'); return; }
    const rid = document.getElementById('avaliacaoReservaId').value;
    const comentario = document.getElementById('avaliacaoComentario').value.trim();
    const user = getCurrentUser();
    const r = (user?.reservas || []).find(x => x.id === rid);
    if (r) {
      r.avaliacao = { estrelas: starSelected, comentario, data: new Date().toLocaleDateString('pt-PT') };
      saveCurrentUser(user);
    }
    document.getElementById('avaliacaoModal').classList.add('hidden');
    document.body.style.overflow = '';
    showToast(`Avaliação de ${starSelected}⭐ submetida! Obrigado pelo feedback. 🙏`);
  });
}

/* =================== CAR-16 — INDISPONIBILIDADE =================== */
function openIndispModal(vid) {
  const user = getCurrentUser();
  const v = (user?.veiculos || []).find(x => x.id === vid);
  if (!v) return;
  document.getElementById('indispVeiculoInfo').textContent = `${v.marca} ${v.modelo} (${v.ano})`;
  document.getElementById('indispVeiculoId').value = vid;
  document.getElementById('indispInicio').value = '';
  document.getElementById('indispFim').value = '';
  renderIndispList(vid);
  document.getElementById('indispModal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
  const today = new Date().toISOString().split('T')[0];
  document.getElementById('indispInicio').min = today;
  document.getElementById('indispFim').min = today;
}

function renderIndispList(vid) {
  const user = getCurrentUser();
  const v = (user?.veiculos || []).find(x => x.id === vid);
  const list = document.getElementById('indispList');
  if (!list || !v) return;
  const periodos = v.indisponibilidade || [];
  if (periodos.length === 0) { list.innerHTML = '<span style="font-size:0.8rem;color:var(--grey-text);">Nenhum período definido.</span>'; return; }
  list.innerHTML = periodos.map((p, i) => `
    <span class="indisp-tag">${p.inicio} → ${p.fim}
      <button onclick="removeIndisp('${vid}',${i})">✕</button>
    </span>
  `).join('');
}

function removeIndisp(vid, idx) {
  const user = getCurrentUser();
  const v = (user.veiculos || []).find(x => x.id === vid);
  if (v) { v.indisponibilidade = (v.indisponibilidade || []).filter((_, i) => i !== idx); saveCurrentUser(user); }
  renderIndispList(vid);
}

function initIndispModal() {
  document.getElementById('indispClose')?.addEventListener('click', () => {
    document.getElementById('indispModal').classList.add('hidden');
    document.body.style.overflow = '';
  });
  document.getElementById('indispConfirm')?.addEventListener('click', () => {
    const inicio = document.getElementById('indispInicio').value;
    const fim    = document.getElementById('indispFim').value;
    const vid    = document.getElementById('indispVeiculoId').value;
    if (!inicio || !fim || fim <= inicio) { showToast('Datas inválidas.', 'error'); return; }
    const user = getCurrentUser();
    const v = (user.veiculos || []).find(x => x.id === vid);
    if (v) {
      v.indisponibilidade = v.indisponibilidade || [];
      v.indisponibilidade.push({ inicio, fim });
      saveCurrentUser(user);
    }
    renderIndispList(vid);
    showToast('Período de indisponibilidade adicionado ✅');
  });
}

/* =================== RENDER CONTA EXTENDIDA (Sprint 3) =================== */
function injectContaSprint3() {
  const wrap = document.querySelector('.conta-grid');
  if (!wrap || document.getElementById('contaReservasCard')) return;

  // Card reservas recebidas (CAR-10)
  const cardRec = document.createElement('div');
  cardRec.className = 'conta-card';
  cardRec.id = 'contaReservasCard';
  cardRec.style.gridColumn = '1 / -1';
  cardRec.innerHTML = `
    <div class="conta-card-header">
      <span class="conta-card-icon">📬</span>
      <span class="conta-card-title">Pedidos de Reserva Recebidos</span>
    </div>
    <div class="conta-card-body" id="contaReservasRecebidasList">
      <div class="dash-empty"><div class="dash-empty-icon">📬</div><p>Sem pedidos pendentes.</p></div>
    </div>`;
  wrap.appendChild(cardRec);

  // Card transações (CAR-37)
  const cardTrans = document.createElement('div');
  cardTrans.className = 'conta-card';
  cardTrans.id = 'contaTransacoesCard';
  cardTrans.style.gridColumn = '1 / -1';
  cardTrans.innerHTML = `
    <div class="conta-card-header">
      <span class="conta-card-icon">📋</span>
      <span class="conta-card-title">Histórico de Transações</span>
    </div>
    <div class="conta-card-body" id="contaTransacoesList">
      <div class="dash-empty"><div class="dash-empty-icon">📋</div><p>Sem transações.</p></div>
    </div>`;
  wrap.appendChild(cardTrans);
}

// Patch renderContaVehicles para incluir botões indisponibilidade
const _origRenderContaVehicles = renderContaVehicles;
function renderContaVehicles() {
  const user = getCurrentUser();
  if (!user) return;
  const veiculos = user.veiculos || [];
  const container = document.getElementById('contaVehiclesList');
  if (!container) return;
  if (veiculos.length === 0) {
    container.innerHTML = `<div class="dash-empty"><div class="dash-empty-icon">🚗</div><p>Ainda não anunciaste nenhum veículo. Clica em <strong>+ Adicionar</strong> para começar.</p></div>`;
    return;
  }
  container.innerHTML = veiculos.map(v => `
    <div class="dash-vehicle-item">
      <div class="dash-vehicle-icon">🚗</div>
      <div class="dash-vehicle-info">
        <div class="dash-vehicle-name">${v.marca} ${v.modelo} (${v.ano})
          ${v.validado === false ? '<span class="dash-reserva-badge badge-cancelled" style="margin-left:6px;">REJEITADO</span>' :
            v.validado ? '<span class="dash-reserva-badge badge-confirmed" style="margin-left:6px;">APROVADO</span>' :
            '<span class="dash-reserva-badge badge-pending" style="margin-left:6px;">PENDENTE</span>'}
        </div>
        <div class="dash-vehicle-sub">${v.combustivel} • ${v.transmissao} • ${v.localizacao} • ${v.km.toLocaleString('pt')} km</div>
      </div>
      <div style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;">
        <div class="dash-vehicle-price">${v.precoDia}€/dia</div>
        <button class="btn btn-outline btn-sm" onclick="openEditVehicle('${v.id}')">✏️</button>
        <button class="btn btn-ghost btn-sm" onclick="openIndispModal('${v.id}')" title="Indisponibilidade">📅</button>
        <button class="reserva-cancel-btn" onclick="removeVehicle('${v.id}')">🗑️</button>
      </div>
    </div>
  `).join('');
}

/* =================== PATCH renderConta =================== */
const _origRenderConta = renderConta;
function renderConta() {
  _origRenderConta();
  injectContaSprint3();
  renderContaReservasRecebidas();
  renderTransacoes();
}

/* =================== PATCH renderReservas — botões KM =================== */
const _origShowReservasTab = showReservasTab;
function showReservasTab(tab) {
  const user = getCurrentUser();
  if (!user) return;
  const today = new Date().toISOString().split('T')[0];
  const all   = user.reservas || [];
  let filtered;
  if (tab === 'ativas')    filtered = all.filter(r => r.estado === 'confirmada' && r.dataFim >= today);
  if (tab === 'futuras')   filtered = all.filter(r => r.estado === 'pendente');
  if (tab === 'historico') filtered = all.filter(r => r.estado === 'cancelada' || r.estado === 'concluida' || (r.dataFim < today && r.estado !== 'confirmada'));

  const container = document.getElementById('reservasList');
  if (!filtered || filtered.length === 0) {
    container.innerHTML = `
      <div class="reservas-empty">
        <div class="reservas-empty-icon">📅</div>
        <h3>Nenhuma reserva aqui</h3>
        <p>As tuas reservas aparecerão aqui após fazeres uma.</p>
      </div>`;
    return;
  }
  container.innerHTML = filtered.map(r => {
    const podeKmInicial = r.estado === 'confirmada' && !r.kmInicial && r.dataInicio <= today;
    const podeKmFinal   = r.estado === 'confirmada' && r.kmInicial  && r.dataFim <= today;
    const podeAvaliar   = r.estado === 'concluida' && !r.avaliacao;
    return `
    <div class="reserva-item">
      <div class="reserva-item-icon">🚗</div>
      <div class="reserva-item-info">
        <div class="reserva-item-name">${r.veiculo}</div>
        <div class="reserva-item-sub"><span class="dash-reserva-badge ${badgeClass(r.estado)}">${r.estado.toUpperCase()}</span>
          ${r.avaliacao ? `<span style="margin-left:6px;font-size:0.75rem;color:#f1c40f;">⭐ ${r.avaliacao.estrelas}/5</span>` : ''}
        </div>
        <div class="reserva-item-dates" style="margin-top:6px;">
          <strong>${r.dataInicio}</strong> → <strong>${r.dataFim}</strong> (${r.dias} dia${r.dias !== 1 ? 's' : ''})
        </div>
      </div>
      <div class="reserva-item-right">
        <div class="reserva-item-price">${r.totalFinal ? r.totalFinal.toFixed(2) : r.total}€</div>
        <div style="display:flex;flex-direction:column;gap:4px;align-items:flex-end;margin-top:6px;">
          ${podeKmInicial ? `<button class="admin-action-btn btn-approve" onclick="openKmInicial('${r.id}')">📍 KM Início</button>` : ''}
          ${podeKmFinal   ? `<button class="admin-action-btn btn-approve" onclick="openKmFinal('${r.id}')">🏁 Devolver</button>` : ''}
          ${podeAvaliar   ? `<button class="admin-action-btn btn-unblock" onclick="openAvaliacaoModal('${r.id}')">⭐ Avaliar</button>` : ''}
          ${(r.estado === 'pendente') ? `<button class="reserva-cancel-btn" onclick="cancelarReserva('${r.id}')">Cancelar</button>` : ''}
        </div>
      </div>
    </div>`;
  }).join('');
}

/* =================== PATCH updateNavbar — mostrar admin =================== */
const _origUpdateNavbar = updateNavbar;
function updateNavbar() {
  _origUpdateNavbar();
  const adminBtn = document.querySelector('.nav-admin-only');
  if (adminBtn) adminBtn.classList.toggle('hidden', !isAdmin());
}

/* =================== PATCH handleLogin =================== */
// Override original handleLogin no init
function patchLoginAfterInit() {
  document.getElementById('loginSubmit')?.removeEventListener('click', handleLogin);
  document.getElementById('loginSubmit')?.addEventListener('click', handleLoginPatched);
}

/* =================== PATCH initSearch → vehicles page =================== */
const _origInitVehiclesPage = initVehiclesPage;
function initVehiclesPage() {
  _origInitVehiclesPage();
  // Inject kms filter after a tick (DOM ready)
  setTimeout(injectKmsFilter, 50);
}

/* =================== KM MODAL INITS =================== */
function initKmModals() {
  document.getElementById('kmInicialClose')?.addEventListener('click', () => {
    document.getElementById('kmInicialModal').classList.add('hidden'); document.body.style.overflow = '';
  });
  document.getElementById('kmInicialModal')?.addEventListener('click', e => {
    if (e.target === e.currentTarget) { document.getElementById('kmInicialModal').classList.add('hidden'); document.body.style.overflow = ''; }
  });
  document.getElementById('kmInicialConfirm')?.addEventListener('click', confirmarKmInicial);

  document.getElementById('kmFinalClose')?.addEventListener('click', () => {
    document.getElementById('kmFinalModal').classList.add('hidden'); document.body.style.overflow = '';
  });
  document.getElementById('kmFinalModal')?.addEventListener('click', e => {
    if (e.target === e.currentTarget) { document.getElementById('kmFinalModal').classList.add('hidden'); document.body.style.overflow = ''; }
  });
  document.getElementById('kmFinalConfirm')?.addEventListener('click', confirmarKmFinal);
  document.getElementById('kmFinalValor')?.addEventListener('input', calcKmFinalPreview);

  document.getElementById('indispModal')?.addEventListener('click', e => {
    if (e.target === e.currentTarget) { document.getElementById('indispModal').classList.add('hidden'); document.body.style.overflow = ''; }
  });
  document.getElementById('avaliacaoModal')?.addEventListener('click', e => {
    if (e.target === e.currentTarget) { document.getElementById('avaliacaoModal').classList.add('hidden'); document.body.style.overflow = ''; }
  });
}

/* =================== PATCH DOMContentLoaded =================== */
document.addEventListener('DOMContentLoaded', () => {
  ensureAdminExists();
  initAdminPage();
  initKmModals();
  initAvaliacaoModal();
  initIndispModal();
  patchLoginAfterInit();
  // Registar página admin
  const adminPage = document.getElementById('admin');
  if (adminPage) router.register('admin', adminPage);
  // Patch saldo modal to record transactions
  const origSaldoConfirm = document.getElementById('saldoModalConfirm');
  if (origSaldoConfirm) {
    origSaldoConfirm.addEventListener('click', () => {
      const user = getCurrentUser();
      if (!user) return;
      const valor = parseFloat(document.getElementById('saldoModalValor').value);
      if (!valor || valor <= 0) return;
      addTransacao(user.email, {
        tipo: saldoAction === 'depositar' ? 'entrada' : 'saida',
        desc: saldoAction === 'depositar' ? 'Depósito de fundos' : 'Levantamento de fundos',
        valor, data: new Date().toLocaleDateString('pt-PT')
      });
    });
  }
});
