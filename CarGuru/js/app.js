/* ============================================
   CARGURU — MAIN JAVASCRIPT
============================================ */

// =================== PAGE ROUTER ===================
const router = {
  pages: {},

  register(id, el) {
    this.pages[id] = el;
  },

  go(id) {
    Object.values(this.pages).forEach(p => p.classList.remove('active'));
    if (this.pages[id]) {
      this.pages[id].classList.add('active');
      window.scrollTo({ top: 0, behavior: 'instant' });
    }
    // Update nav active state
    document.querySelectorAll('[data-nav]').forEach(link => {
      link.classList.toggle('active', link.dataset.nav === id);
    });
  }
};

// =================== TOAST ===================
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
  setTimeout(() => { toast.style.opacity = '0'; toast.style.transform = 'translateX(100%)'; toast.style.transition = '0.3s ease'; setTimeout(() => toast.remove(), 300); }, 4000);
}

// =================== NAVBAR ===================
function initNavbar() {
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    navbar.classList.toggle('scrolled', window.scrollY > 20);
  });
}

// =================== AUTH ===================
const mockUsers = JSON.parse(localStorage.getItem('carguru_users') || '[]');

function initAuth() {
  const tabs = document.querySelectorAll('.auth-tab');
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');

  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      const target = tab.dataset.tab;
      loginForm.classList.toggle('hidden', target !== 'login');
      registerForm.classList.toggle('hidden', target !== 'register');
    });
  });

  // Password toggles
  document.querySelectorAll('.password-toggle').forEach(btn => {
    btn.addEventListener('click', () => {
      const input = btn.previousElementSibling;
      const isPass = input.type === 'password';
      input.type = isPass ? 'text' : 'password';
      btn.textContent = isPass ? '🙈' : '👁️';
    });
  });

  // Password strength
  const regPassword = document.getElementById('regPassword');
  if (regPassword) {
    regPassword.addEventListener('input', () => {
      const val = regPassword.value;
      const fill = document.getElementById('strengthFill');
      const text = document.getElementById('strengthText');
      let strength = 'weak', label = 'Fraca';
      if (val.length > 5) { strength = 'fair'; label = 'Razoável'; }
      if (val.length > 7 && /[A-Z]/.test(val)) { strength = 'good'; label = 'Boa'; }
      if (val.length > 9 && /[A-Z]/.test(val) && /[0-9]/.test(val) && /[^a-zA-Z0-9]/.test(val)) { strength = 'strong'; label = 'Forte 💪'; }
      fill.className = `strength-fill ${strength}`;
      text.textContent = val.length ? `Segurança: ${label}` : '';
    });
  }

  // Login submit
  document.getElementById('loginSubmit')?.addEventListener('click', handleLogin);
  document.getElementById('registerSubmit')?.addEventListener('click', handleRegister);
}

function handleLogin() {
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;

  if (!email || !password) {
    showToast('Preenche todos os campos.', 'error'); return;
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    showToast('Email inválido.', 'error'); return;
  }

  const users = JSON.parse(localStorage.getItem('carguru_users') || '[]');
  const user = users.find(u => u.email === email && u.password === btoa(password));

  if (user) {
    localStorage.setItem('carguru_session', JSON.stringify({ name: user.name, email: user.email }));
    showToast(`Bem-vindo de volta, ${user.name}! 🚗`);
    setTimeout(() => router.go('home'), 1200);
  } else {
    showToast('Email ou password incorretos.', 'error');
  }
}

function handleRegister() {
  const name     = document.getElementById('regName').value.trim();
  const email    = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value;
  const confirm  = document.getElementById('regConfirm').value;
  const terms    = document.getElementById('regTerms').checked;

  if (!name || !email || !password || !confirm) {
    showToast('Preenche todos os campos.', 'error'); return;
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    showToast('Email inválido.', 'error'); return;
  }
  if (password.length < 6) {
    showToast('A password deve ter pelo menos 6 caracteres.', 'error'); return;
  }
  if (password !== confirm) {
    showToast('As passwords não coincidem.', 'error'); return;
  }
  if (!terms) {
    showToast('Aceita os termos para continuar.', 'error'); return;
  }

  const users = JSON.parse(localStorage.getItem('carguru_users') || '[]');
  if (users.find(u => u.email === email)) {
    showToast('Este email já está registado.', 'error'); return;
  }

  users.push({ name, email, password: btoa(password), createdAt: new Date().toISOString() });
  localStorage.setItem('carguru_users', JSON.stringify(users));
  localStorage.setItem('carguru_session', JSON.stringify({ name, email }));

  showToast(`Conta criada com sucesso! Bem-vindo, ${name}! 🎉`);
  setTimeout(() => router.go('home'), 1200);
}

// =================== SEARCH FILTER ===================
function initSearch() {
  const btn = document.getElementById('searchBtn');
  if (btn) {
    btn.addEventListener('click', () => {
      const marca = document.getElementById('searchMarca').value;
      const tipo  = document.getElementById('searchTipo').value;
      const preco = document.getElementById('searchPreco').value;
      showToast(`A pesquisar: ${marca || 'Todas as marcas'}, ${tipo || 'Todos os tipos'}${preco ? ', até ' + preco + '€' : ''} 🔍`);
    });
  }
}

// =================== SCROLL ANIMATIONS ===================
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

// =================== MOBILE MENU ===================
function initMobileMenu() {
  const hamburger = document.getElementById('hamburger');
  const navLinks = document.getElementById('navLinks');
  if (hamburger && navLinks) {
    hamburger.addEventListener('click', () => {
      const open = navLinks.classList.toggle('mobile-open');
      hamburger.textContent = open ? '✕' : '☰';
    });
  }
}

// =================== INIT ===================
document.addEventListener('DOMContentLoaded', () => {
  // Register pages
  document.querySelectorAll('.page').forEach(page => {
    router.register(page.id, page);
  });

  // Nav links
  document.querySelectorAll('[data-nav]').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      router.go(link.dataset.nav);
    });
  });

  initNavbar();
  initAuth();
  initSearch();
  initScrollAnimations();
  initMobileMenu();

  // Start on home
  router.go('home');
});
