/** Login del dentista — mismo diseño que login.php, autenticación con auth-service (JWT). */
import { apiAuth, sesion } from './api.js';

const $ = (id) => document.getElementById(id);

function mostrarPantalla(id) {
  document.querySelectorAll('.cs').forEach((c) => c.classList.remove('active'));
  $(id).classList.add('active');
}

function mensaje(id, texto) {
  const caja = $(id);
  caja.textContent = texto || '';
  caja.classList.toggle('show', Boolean(texto));
}

/* Si ya hay sesión válida, entrar directo al panel. */
if (sesion.token()) {
  apiAuth.verificar().then(() => location.replace('/dentista/')).catch(() => sesion.cerrar());
}

/* Enlace del correo de recuperación: /dentista/login.html?token=... */
const tokenUrl = new URLSearchParams(location.search).get('token');
if (tokenUrl) {
  $('i-codigo').value = tokenUrl;
  mostrarPantalla('cs-reset');
}

$('form-login').addEventListener('submit', async (e) => {
  e.preventDefault();
  mensaje('login-err', '');
  const correo = $('i-correo').value.trim();
  const password = $('i-pw').value;
  if (!correo || !password) {
    mensaje('login-err', 'Ingresa tu correo y tu contraseña.');
    return;
  }
  const boton = $('btn-login');
  boton.disabled = true;
  boton.innerHTML = '<div class="spinner"></div> Verificando...';
  try {
    const { token, nombre } = await apiAuth.login(correo, password);
    sesion.guardar(token, nombre, $('i-recordar').checked);
    location.replace('/dentista/');
  } catch (err) {
    mensaje('login-err', err.message);
    $('i-pw').classList.add('err');
  } finally {
    boton.disabled = false;
    boton.textContent = 'Ingresar al panel →';
  }
});

$('i-pw').addEventListener('input', () => $('i-pw').classList.remove('err'));

$('btn-ojo').addEventListener('click', () => {
  const input = $('i-pw');
  input.type = input.type === 'password' ? 'text' : 'password';
});

$('btn-rec').addEventListener('click', async () => {
  mensaje('rec-ok', '');
  mensaje('rec-err', '');
  const correo = $('i-rec').value.trim();
  if (!correo || !$('i-rec').checkValidity()) {
    mensaje('rec-err', 'Escribe un correo válido.');
    return;
  }
  try {
    const r = await apiAuth.recuperar(correo);
    mensaje('rec-ok', `✅ ${r.msg}`);
  } catch (err) {
    mensaje('rec-err', err.message);
  }
});

$('btn-reset').addEventListener('click', async () => {
  mensaje('reset-ok', '');
  mensaje('reset-err', '');
  const nueva = $('i-nueva').value;
  if (nueva !== $('i-confirma').value) {
    mensaje('reset-err', 'Las contraseñas no coinciden.');
    return;
  }
  try {
    const r = await apiAuth.restablecer($('i-codigo').value.trim(), nueva);
    mensaje('reset-ok', `✅ ${r.msg}`);
    setTimeout(() => mostrarPantalla('cs-login'), 2000);
  } catch (err) {
    mensaje('reset-err', err.message);
  }
});

document.addEventListener('click', (e) => {
  const destino = e.target.closest('[data-cs]');
  if (destino) {
    e.preventDefault();
    mostrarPantalla(destino.dataset.cs);
  }
});
