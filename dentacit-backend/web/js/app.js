/**
 * Vista del paciente — DentaCit.
 * Mismo flujo que el catalogo.php original (servicio → horario → datos → confirmación,
 * Mis citas y Perfil), pero los datos vienen de los microservicios por medio del API Gateway.
 */
import {
  apiPaciente, escapar, precio, hora12, partesFecha, fechaLarga, hoyIso, iniciales,
} from './api.js';

const $ = (id) => document.getElementById(id);

const estado = {
  servicios: [],
  servicio: null,   // {id, nombre, costo}
  fecha: '',
  hora: '',         // "09:00"
  citas: [],
};

/* ---------- Datos del paciente guardados en este dispositivo ---------- */

const CLAVE_PACIENTE = 'dentacit-paciente';
const paciente = {
  leer() {
    try { return JSON.parse(localStorage.getItem(CLAVE_PACIENTE)) || {}; } catch { return {}; }
  },
  guardar(datos) {
    try { localStorage.setItem(CLAVE_PACIENTE, JSON.stringify({ ...paciente.leer(), ...datos })); } catch { /* sin almacenamiento */ }
  },
  borrar() {
    try { localStorage.removeItem(CLAVE_PACIENTE); } catch { /* nada */ }
  },
};

/* ---------- Navegación entre pantallas y pasos ---------- */

const NAV_DE_PANTALLA = { 'scr-home': 'nav-home', 'scr-confirm': 'nav-home', 'scr-citas': 'nav-citas', 'scr-perfil': 'nav-perfil' };

function irA(id) {
  document.querySelectorAll('.screen').forEach((s) => s.classList.remove('active'));
  $(id).classList.add('active');
  document.querySelectorAll('.nav-item').forEach((n) => n.classList.remove('active'));
  $(NAV_DE_PANTALLA[id]).classList.add('active');
  window.scrollTo({ top: 0, behavior: 'smooth' });
  if (id === 'scr-citas') cargarMisCitas();
  if (id === 'scr-perfil') pintarPerfil();
  if (id === 'scr-home') irPaso(1);
}

function irPaso(n) {
  document.querySelectorAll('.step-section').forEach((s) => s.classList.remove('active'));
  $(`step${n}`).classList.add('active');
  for (let i = 1; i <= 3; i++) {
    const e = $(`si${i}`);
    e.classList.remove('active', 'done');
    if (i < n) e.classList.add('done');
    if (i === n) e.classList.add('active');
  }
  if (n === 3) {
    rellenarDatos();
    revisarFormulario();
  }
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function mostrarAlerta(mensaje) {
  const alerta = $('alerta');
  alerta.textContent = mensaje ? `⚠️ ${mensaje}` : '';
  alerta.style.display = mensaje ? 'flex' : 'none';
  if (mensaje) window.scrollTo({ top: 0, behavior: 'smooth' });
}

function toast(mensaje) {
  const t = document.createElement('div');
  t.className = 'toast-p';
  t.textContent = mensaje;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}

/* ---------- Paso 1: servicios ---------- */

async function cargarServicios() {
  const grid = $('services-grid');
  try {
    estado.servicios = await apiPaciente.servicios();
  } catch (e) {
    grid.innerHTML = `<p style="color:var(--red);font-size:13px;grid-column:1/-1;">${escapar(e.message)}</p>`;
    return;
  }
  if (estado.servicios.length === 0) {
    grid.innerHTML = '<p style="color:var(--text-muted);font-size:13px;grid-column:1/-1;">Por el momento no hay servicios disponibles.</p>';
    return;
  }
  grid.innerHTML = estado.servicios.map((s) => `
    <div class="service-card" data-servicio="${s.id}" role="button" tabindex="0">
      <div class="svc-check">✓</div>
      <div class="svc-name">${escapar(s.nombre)}</div>
      <div class="svc-price">${precio(s.costo)}</div>
    </div>`).join('');
}

function elegirServicio(id) {
  estado.servicio = estado.servicios.find((s) => s.id === Number(id)) || null;
  document.querySelectorAll('.service-card').forEach((c) => {
    c.classList.toggle('selected', Number(c.dataset.servicio) === estado.servicio?.id);
  });
  $('btn1').disabled = !estado.servicio;
}

/* ---------- Paso 2: fecha y bloque horario ---------- */

async function cargarDiasProximos() {
  const contenedor = $('dias-proximos');
  try {
    const dias = await apiPaciente.proximosDias();
    contenedor.innerHTML = dias.length
      ? dias.slice(0, 10).map(({ fecha }) => {
        const { dia, mes } = partesFecha(fecha);
        return `<button type="button" class="dia-chip${fecha === estado.fecha ? ' on' : ''}" data-dia="${fecha}">${dia} ${mes}</button>`;
      }).join('')
      : '<span class="field-hint">No hay horarios disponibles por ahora.</span>';
  } catch {
    contenedor.innerHTML = '';
  }
}

async function cambiarFecha(fecha) {
  estado.fecha = fecha;
  estado.hora = '';
  $('fecha-inp').value = fecha;
  $('btn2').disabled = true;
  document.querySelectorAll('.dia-chip').forEach((c) => c.classList.toggle('on', c.dataset.dia === fecha));

  const grid = $('slot-grid');
  if (!fecha) return;
  grid.innerHTML = '<p style="color:var(--text-muted);font-size:13px;grid-column:1/-1;">Cargando...</p>';
  try {
    const { bloques } = await apiPaciente.bloquesDelDia(fecha);
    if (bloques.length === 0) {
      grid.innerHTML = '<p style="color:var(--text-muted);font-size:13px;grid-column:1/-1;">El dentista no atiende ese día. Elige otra fecha.</p>';
      return;
    }
    grid.innerHTML = bloques.map((b) => `
      <div class="slot ${b.libre ? 'available' : 'blocked'}" ${b.libre ? `data-hora="${b.hora}" role="button" tabindex="0"` : ''}>
        <span class="slot-lbl">${hora12(b.hora)}</span>
        <span class="slot-sub">${b.libre ? 'Libre' : 'Ocupado'}</span>
      </div>`).join('');
  } catch (e) {
    grid.innerHTML = `<p style="color:var(--red);font-size:13px;grid-column:1/-1;">${escapar(e.message)}</p>`;
  }
}

function elegirHora(hora) {
  estado.hora = hora;
  document.querySelectorAll('.slot').forEach((s) => s.classList.toggle('selected-slot', s.dataset.hora === hora));
  $('btn2').disabled = false;
}

/* ---------- Paso 3: datos y envío ---------- */

function rellenarDatos() {
  const guardado = paciente.leer();
  if (!$('inp-nombre').value && guardado.nombre) $('inp-nombre').value = guardado.nombre;
  if (!$('inp-email').value && guardado.correo) $('inp-email').value = guardado.correo;
  if (!$('inp-tel').value && guardado.telefono) $('inp-tel').value = guardado.telefono;
}

function revisarFormulario() {
  const n = $('inp-nombre').value.trim();
  const e = $('inp-email').value.trim();
  const s = $('inp-sint').value.trim();
  const ok = n.length > 2 && $('inp-email').checkValidity() && e.includes('@') && s.length > 3;
  $('btn-enviar').disabled = !ok;
  $('resumen').style.display = ok ? 'block' : 'none';
  if (ok) {
    $('r-servicio').textContent = estado.servicio?.nombre ?? '—';
    $('r-precio').textContent = estado.servicio ? precio(estado.servicio.costo) : '—';
    $('r-fecha').textContent = estado.fecha ? fechaLarga(estado.fecha) : '—';
    $('r-hora').textContent = estado.hora ? hora12(estado.hora) : '—';
  }
}

async function enviarCita(evento) {
  evento.preventDefault();
  mostrarAlerta('');
  const datos = {
    id_servicio: estado.servicio?.id,
    fecha: estado.fecha,
    hora: estado.hora,
    nombre: $('inp-nombre').value.trim(),
    correo: $('inp-email').value.trim(),
    telefono: $('inp-tel').value.trim(),
    sintomas: $('inp-sint').value.trim(),
  };
  const boton = $('btn-enviar');
  boton.disabled = true;
  boton.textContent = 'Enviando...';
  try {
    await apiPaciente.solicitarCita(datos);
    paciente.guardar({ nombre: datos.nombre, correo: datos.correo, telefono: datos.telefono });
    mostrarConfirmacion(datos);
  } catch (e) {
    mostrarAlerta(e.message);
    if (e.estado === 422 && /horario|disponible|atiende/i.test(e.message)) {
      irPaso(2);
      cambiarFecha(estado.fecha);
      cargarDiasProximos();
    }
  } finally {
    boton.textContent = 'Enviar solicitud de cita ✓';
    revisarFormulario();
  }
}

function mostrarConfirmacion(datos) {
  const { dia, mes } = partesFecha(datos.fecha);
  $('c-day').textContent = dia;
  $('c-month').textContent = mes;
  $('c-serv').textContent = estado.servicio?.nombre ?? '';
  $('c-det').textContent = `${hora12(datos.hora)} · ${precio(estado.servicio?.costo ?? 0)}`;

  $('form-cita').reset();
  $('inp-sint').value = '';
  estado.servicio = null;
  estado.fecha = '';
  estado.hora = '';
  document.querySelectorAll('.service-card').forEach((c) => c.classList.remove('selected'));
  $('btn1').disabled = true;
  $('slot-grid').innerHTML = '<p style="color:var(--text-muted);font-size:13px;grid-column:1/-1;">Primero selecciona una fecha.</p>';

  document.querySelectorAll('.screen').forEach((s) => s.classList.remove('active'));
  $('scr-confirm').classList.add('active');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

/* ---------- Mis citas ---------- */

function costoDe(cita) {
  const s = estado.servicios.find((x) => x.id === cita.id_servicio);
  return s ? ` · ${precio(s.costo)}` : '';
}

function tarjetaCita(c) {
  const { dia, mes } = partesFecha(c.fecha);
  const rechazada = c.estatus === 'Rechazada';
  const aceptada = c.estatus === 'Aceptada';
  const estiloCaja = rechazada ? 'background:var(--red-light);border-color:#fecaca;' : '';
  const estiloTexto = rechazada ? 'color:var(--red);' : '';
  const badge = aceptada ? 'badge-ok' : rechazada ? 'badge-no' : 'badge-pend';
  return `
    <div class="appt-card">
      <div class="date-box" style="${estiloCaja}">
        <div class="date-day" style="${estiloTexto}">${dia}</div>
        <div class="date-month" style="${estiloTexto}">${mes}</div>
      </div>
      <div class="appt-info">
        <div class="appt-service">${escapar(c.servicio)}</div>
        <div class="appt-detail">${hora12(c.hora)}${costoDe(c)}</div>
        ${c.correo_enviado && !c.estatus.startsWith('P') ? `<div class="mail-ok">📧 ${aceptada ? 'Correo de confirmación enviado' : 'Te avisamos por correo'}</div>` : ''}
      </div>
      <span class="badge ${badge}">${escapar(c.estatus)}</span>
    </div>`;
}

async function cargarMisCitas() {
  const correo = paciente.leer().correo;
  const input = $('consulta-correo');
  if (correo && !input.value) input.value = correo;
  if (!input.value.trim()) {
    estado.citas = [];
    pintarMisCitas('Escribe el correo con el que agendaste para ver tus citas.');
    return;
  }
  $('tab-act').innerHTML = '<div class="empty-msg">Cargando...</div>';
  try {
    estado.citas = await apiPaciente.misCitas(input.value.trim());
    pintarMisCitas();
  } catch (e) {
    $('tab-act').innerHTML = `<div class="alert alert-err">⚠️ ${escapar(e.message)}</div>`;
  }
}

function pintarMisCitas(mensajeVacio) {
  const activas = estado.citas.filter((c) => c.estatus === 'Pendiente');
  const historial = estado.citas.filter((c) => c.estatus !== 'Pendiente');
  const notaCorreo = `
    <div class="email-note"><span>📧</span><div>
      <span class="email-note-bold">¿Cuándo responden?</span>
      Recibirás un correo automático con la respuesta del dentista.
    </div></div>`;

  $('tab-act').innerHTML = activas.length
    ? activas.map(tarjetaCita).join('') + `
      <div class="block-card">
        <h3>🔒 No puedes agendar otra cita</h3>
        <p>Espera a que tu solicitud sea confirmada o rechazada.</p>
      </div>` + notaCorreo
    : `<div class="empty-msg">${escapar(mensajeVacio || 'No tienes citas activas.')}<br>
        <button class="btn-small" data-ir="scr-home">Agendar una cita</button></div>` + notaCorreo;

  $('tab-hist').innerHTML = historial.length
    ? historial.map(tarjetaCita).join('')
    : '<div class="empty-msg">No hay citas en el historial aún.</div>';
}

function cambiarPestana(id) {
  document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === id));
  ['tab-act', 'tab-hist'].forEach((t) => { $(t).style.display = t === id ? 'block' : 'none'; });
}

/* ---------- Perfil ---------- */

async function pintarPerfil() {
  const p = paciente.leer();
  $('av-initials').textContent = iniciales(p.nombre) || '👤';
  $('av-nombre').textContent = p.nombre || 'Mi perfil';
  $('av-email').textContent = p.correo || '—';
  $('v-nombre').textContent = p.nombre || '—';
  $('v-email').textContent = p.correo || '—';
  $('v-tel').textContent = p.telefono || '—';

  if (p.correo) {
    try { estado.citas = await apiPaciente.misCitas(p.correo); } catch { /* se muestran los datos guardados */ }
  }
  const total = estado.citas.length;
  $('st-total').textContent = total;
  $('st-acept').textContent = estado.citas.filter((c) => c.estatus === 'Aceptada').length;
  $('st-pend').textContent = estado.citas.filter((c) => c.estatus === 'Pendiente').length;
  $('v-total').textContent = `${total} cita${total !== 1 ? 's' : ''}`;
}

let editando = false;
function alternarEdicion(activar) {
  editando = activar;
  $('view-mode').style.display = editando ? 'none' : 'block';
  $('edit-mode').style.display = editando ? 'block' : 'none';
  $('edit-btn').textContent = editando ? 'Cancelar' : 'Editar';
  if (editando) {
    const p = paciente.leer();
    $('e-nombre').value = p.nombre || '';
    $('e-email').value = p.correo || '';
    $('e-tel').value = p.telefono || '';
  }
}

function guardarPerfil() {
  const nombre = $('e-nombre').value.trim();
  const correo = $('e-email').value.trim();
  if (correo && !$('e-email').checkValidity()) {
    toast('⚠️ Escribe un correo válido');
    return;
  }
  paciente.guardar({ nombre, correo, telefono: $('e-tel').value.trim() });
  $('consulta-correo').value = correo;
  alternarEdicion(false);
  pintarPerfil();
  toast('✅ Datos actualizados');
}

function olvidarDatos() {
  paciente.borrar();
  estado.citas = [];
  $('consulta-correo').value = '';
  ['inp-nombre', 'inp-email', 'inp-tel'].forEach((id) => { $(id).value = ''; });
  $('modal-logout').classList.remove('open');
  irA('scr-home');
  toast('✅ Sesión cerrada correctamente');
}

/* ---------- Eventos ---------- */

document.addEventListener('click', (e) => {
  const destino = e.target.closest('[data-ir]');
  if (destino) irA(destino.dataset.ir);

  const tarjeta = e.target.closest('[data-servicio]');
  if (tarjeta) elegirServicio(tarjeta.dataset.servicio);

  const chip = e.target.closest('[data-dia]');
  if (chip) cambiarFecha(chip.dataset.dia);

  const slot = e.target.closest('[data-hora]');
  if (slot) elegirHora(slot.dataset.hora);

  const paso = e.target.closest('[data-paso]');
  if (paso) irPaso(Number(paso.dataset.paso));

  const pestana = e.target.closest('[data-tab]');
  if (pestana) cambiarPestana(pestana.dataset.tab);
});

document.addEventListener('keydown', (e) => {
  if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('[data-servicio], [data-hora]')) {
    e.preventDefault();
    e.target.click();
  }
});

$('btn1').addEventListener('click', () => {
  if (!estado.servicio) return;
  irPaso(2);
  cargarDiasProximos();
});
$('btn2').addEventListener('click', () => irPaso(3));
$('fecha-inp').addEventListener('change', (e) => cambiarFecha(e.target.value));
['inp-nombre', 'inp-email', 'inp-sint'].forEach((id) => $(id).addEventListener('input', revisarFormulario));
$('form-cita').addEventListener('submit', enviarCita);
$('form-consulta').addEventListener('submit', (e) => {
  e.preventDefault();
  const correo = $('consulta-correo').value.trim();
  if (!$('consulta-correo').checkValidity() || !correo) {
    toast('⚠️ Escribe un correo válido');
    return;
  }
  if (!paciente.leer().correo) paciente.guardar({ correo });
  cargarMisCitas();
});
$('edit-btn').addEventListener('click', () => alternarEdicion(!editando));
$('btn-cancel-edit').addEventListener('click', () => alternarEdicion(false));
$('btn-save-edit').addEventListener('click', guardarPerfil);
$('btn-olvidar').addEventListener('click', () => $('modal-logout').classList.add('open'));
$('btn-logout-cancel').addEventListener('click', () => $('modal-logout').classList.remove('open'));
$('btn-logout-ok').addEventListener('click', olvidarDatos);
$('btn-qr').addEventListener('click', (e) => { e.preventDefault(); $('modal-qr').classList.add('open'); });
$('btn-qr-cerrar').addEventListener('click', () => $('modal-qr').classList.remove('open'));

$('fecha-inp').min = hoyIso();
cargarServicios();
