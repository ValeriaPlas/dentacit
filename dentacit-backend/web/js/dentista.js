/**
 * Panel del dentista — mismo diseño que dashboard.php, conectado a los microservicios
 * (citas-service, catalogo-service, notif-service) con el token JWT de auth-service.
 */
import {
  apiAuth, apiDentista, sesion, escapar, precio, hora12, fechaCorta, iniciales, hoyIso, MESES,
} from './api.js';

const $ = (id) => document.getElementById(id);
const mayuscula = (t) => t.charAt(0).toUpperCase() + t.slice(1);

const estado = {
  citas: [],
  servicios: [],
  horarios: [],
  servicioEnEdicion: null,
};

/* ---------- Sesión ---------- */

function salir() {
  sesion.cerrar();
  location.replace('/dentista/login.html');
}

/** Ejecuta una llamada al API; si el token expiró, regresa al login. */
async function conSesion(promesa) {
  try {
    return await promesa;
  } catch (e) {
    if (e.estado === 401) salir();
    throw e;
  }
}

/* ---------- Utilidades de interfaz ---------- */

function toast(mensaje, esCorreo = false) {
  const t = $('toast-el');
  t.textContent = mensaje;
  t.className = `toast${esCorreo ? ' mail' : ''} show`;
  clearTimeout(toast.temporizador);
  toast.temporizador = setTimeout(() => t.classList.remove('show'), 3500);
}

function mostrarPantalla(id) {
  document.querySelectorAll('.screen').forEach((s) => s.classList.remove('active'));
  $(id).classList.add('active');
  document.querySelectorAll('.sb-item').forEach((i) => i.classList.toggle('active', i.dataset.scr === id));
  $('sidebar').classList.remove('open');
  window.scrollTo({ top: 0 });
}

const abrirModal = (id) => $(id).classList.add('open');
const cerrarModal = (id) => $(id).classList.remove('open');

function errorModal(id, texto) {
  $(id).textContent = texto || '';
  $(id).classList.toggle('show', Boolean(texto));
}

/* ---------- Carga de datos ---------- */

async function cargarCitas() {
  estado.citas = await conSesion(apiDentista.citas());
  // Pendientes primero; dentro de cada grupo, por fecha y hora.
  estado.citas.sort((a, b) => (a.estatus === 'Pendiente' ? 0 : 1) - (b.estatus === 'Pendiente' ? 0 : 1)
    || `${a.fecha} ${a.hora}`.localeCompare(`${b.fecha} ${b.hora}`));
  pintarDashboard();
  pintarSolicitudes();
  pintarCorreos();
  pintarCalendario();
}

async function cargarServicios() {
  estado.servicios = await conSesion(apiDentista.servicios());
  pintarServicios();
}

async function cargarHorarios() {
  estado.horarios = await conSesion(apiDentista.horarios());
  pintarHorarios();
}

/* ---------- Citas ---------- */

function tarjetaCita(c, conCosto) {
  const clase = c.estatus.toLowerCase();
  const servicio = estado.servicios.find((s) => s.id === c.id_servicio);
  return `
    <div class="cita ${clase}">
      <div class="cita-av">${escapar(iniciales(c.paciente_nombre))}</div>
      <div class="cita-info">
        <div class="cita-name">
          ${escapar(c.paciente_nombre)}
          <span class="badge b${clase[0]}">${escapar(c.estatus)}</span>
        </div>
        <div class="cita-det">
          ${escapar(c.servicio)} · ${fechaCorta(c.fecha)} · ${escapar(c.hora)}${conCosto && servicio ? ` · ${precio(servicio.costo)}` : ''}
        </div>
        ${c.sintomas ? `<div class="cita-sint">"${escapar(c.sintomas)}"</div>` : ''}
        ${c.correo_enviado ? '<div class="mail-sent">📧 Correo enviado</div>' : ''}
      </div>
      ${c.estatus === 'Pendiente' ? `
      <div class="cita-acts">
        <button class="ab ab-a" data-responder="${c.id}" data-estatus="Aceptada">✓ Aceptar</button>
        <button class="ab ab-r" data-responder="${c.id}" data-estatus="Rechazada">✕ Rechazar</button>
      </div>` : ''}
    </div>`;
}

function pintarDashboard() {
  const pendientes = estado.citas.filter((c) => c.estatus === 'Pendiente').length;
  $('m-pend').textContent = pendientes;
  $('m-pend-nota').textContent = pendientes > 0 ? 'Requieren acción' : 'Al día ✓';
  $('m-acept').textContent = estado.citas.filter((c) => c.estatus === 'Aceptada').length;
  $('m-correos').textContent = estado.citas.filter((c) => c.correo_enviado).length;
  $('m-total').textContent = estado.citas.length;

  $('pend-indicador').innerHTML = pendientes > 0 ? `<div class="notif-dot"></div>${pendientes} pendientes` : 'Al día ✓';
  const badge = $('sb-badge');
  badge.textContent = pendientes;
  badge.style.display = pendientes > 0 ? 'flex' : 'none';

  $('lista-recientes').innerHTML = estado.citas.length
    ? estado.citas.slice(0, 5).map((c) => tarjetaCita(c, false)).join('')
    : '<p class="vacio">No hay citas aún.<br>Comparte tu QR para recibir solicitudes.</p>';
}

function pintarSolicitudes() {
  const filtro = $('filtro-estatus').value;
  const lista = filtro ? estado.citas.filter((c) => c.estatus === filtro) : estado.citas;
  const nombres = { '': 'Todas las solicitudes', Pendiente: 'Solicitudes pendientes', Aceptada: 'Citas aceptadas', Rechazada: 'Citas rechazadas' };
  $('titulo-filtro').textContent = nombres[filtro];
  $('conteo-filtro').textContent = `${lista.length} en total`;
  $('lista-solicitudes').innerHTML = lista.length
    ? lista.map((c) => tarjetaCita(c, true)).join('')
    : '<p class="vacio">No hay citas con ese estado.</p>';
}

async function responderCita(id, estatus, boton) {
  boton.closest('.cita-acts')?.querySelectorAll('button').forEach((b) => { b.disabled = true; });
  try {
    const r = await conSesion(apiDentista.responder(id, estatus));
    if (estatus === 'Aceptada') {
      toast(r.correo_enviado ? '📧 Cita aceptada · Correo enviado al paciente' : '✓ Cita aceptada (el correo no se pudo enviar)', true);
    } else {
      toast(r.correo_enviado ? '✕ Cita rechazada · Se avisó al paciente' : '✕ Cita rechazada');
    }
    await cargarCitas();
  } catch (e) {
    toast(`❌ ${e.message}`);
    await cargarCitas();
  }
}

function pintarCorreos() {
  const enviados = estado.citas.filter((c) => c.correo_enviado);
  $('conteo-correos').textContent = `${enviados.length} enviados`;
  $('lista-correos').innerHTML = enviados.length
    ? enviados.map((c) => `
      <div class="ml-item">
        <div class="ml-icon">${c.estatus === 'Aceptada' ? '✅' : '✕'}</div>
        <div style="flex:1;">
          <div class="ml-to">${escapar(c.paciente_nombre)} · ${escapar(c.paciente_correo)}</div>
          <div class="ml-det">${escapar(c.servicio)} · ${fechaCorta(c.fecha)} — ${c.estatus === 'Aceptada' ? 'Confirmación enviada' : 'Aviso de rechazo enviado'}</div>
        </div>
        <div class="ml-time">${escapar(c.hora)}</div>
      </div>`).join('')
    : '<p class="vacio">No se han enviado correos aún.</p>';
}

/* ---------- Calendario navegable ---------- */

const hoy = new Date();
let calAnio = hoy.getFullYear();
let calMes = hoy.getMonth();
let diaSeleccionado = null;

function pintarCalendario() {
  const titulo = `${MESES[calMes]} ${calAnio}`;
  $('cal-mes-label').textContent = titulo;
  $('cal-titulo').textContent = titulo;
  const fechasConCitas = new Set(estado.citas.filter((c) => c.estatus !== 'Rechazada').map((c) => c.fecha));

  const grid = $('cal-grid');
  grid.querySelectorAll('.cd').forEach((c) => c.remove());
  const primerDia = new Date(calAnio, calMes, 1).getDay();
  const desfase = primerDia === 0 ? 6 : primerDia - 1;
  const diasEnMes = new Date(calAnio, calMes + 1, 0).getDate();

  for (let i = 0; i < desfase; i++) {
    const vacio = document.createElement('div');
    vacio.className = 'cd dim';
    grid.appendChild(vacio);
  }
  for (let d = 1; d <= diasEnMes; d++) {
    const fecha = `${calAnio}-${String(calMes + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    const celda = document.createElement('div');
    celda.className = 'cd';
    if (fecha === hoyIso()) celda.classList.add('today');
    if (fechasConCitas.has(fecha)) celda.classList.add('hc');
    if (fecha === diaSeleccionado) celda.classList.add('sel');
    celda.dataset.fecha = fecha;
    celda.textContent = d;
    grid.appendChild(celda);
  }
}

function mostrarDia(fecha) {
  diaSeleccionado = fecha;
  pintarCalendario();
  const delDia = estado.citas.filter((c) => c.fecha === fecha && c.estatus !== 'Rechazada');
  $('cal-detalle').innerHTML = delDia.length
    ? `<div style="font-size:11px;font-weight:700;margin-bottom:6px;">${fechaCorta(fecha)}</div>`
      + delDia.map((c) => `<div class="ml-item"><div class="ml-icon">${c.estatus === 'Aceptada' ? '✅' : '🔔'}</div><div style="flex:1;"><div class="ml-to">${escapar(c.hora)} · ${escapar(c.paciente_nombre)}</div><div class="ml-det">${escapar(c.servicio)} · ${escapar(c.estatus)}</div></div></div>`).join('')
    : `<p style="font-size:11px;color:var(--text-muted);">Sin citas el ${fechaCorta(fecha)}.</p>`;
}

/* ---------- Horarios ---------- */

function pintarHorarios() {
  $('conteo-horarios').textContent = `${estado.horarios.length} bloque${estado.horarios.length !== 1 ? 's' : ''}`;
  if (estado.horarios.length === 0) {
    $('lista-horarios').innerHTML = '<p class="vacio">No hay bloques definidos. Los pacientes no podrán agendar hasta que agregues uno.</p>';
    return;
  }
  const porDia = {};
  estado.horarios.forEach((h) => { (porDia[h.fecha] ||= []).push(h); });
  const formato = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' });
  $('lista-horarios').innerHTML = Object.entries(porDia).map(([fecha, bloques]) => `
    <div class="dia-hor">
      <div class="dia-hor-tit">${escapar(mayuscula(formato.format(new Date(`${fecha}T00:00:00Z`))))}</div>
      <div class="tslot-grid">
        ${bloques.map((h) => `
          <div class="tslot on">
            ${hora12(h.hora_inicio)} – ${hora12(h.hora_fin)}<span>Activo</span>
            <button class="del" data-borrar-horario="${h.id}" aria-label="Eliminar bloque">✕</button>
          </div>`).join('')}
      </div>
    </div>`).join('');
}

async function guardarHorario() {
  errorModal('mh-err', '');
  const boton = $('mh-guardar');
  boton.disabled = true;
  try {
    await conSesion(apiDentista.crearHorario({
      fecha: $('mh-fecha').value,
      hora_inicio: $('mh-inicio').value,
      hora_fin: $('mh-fin').value,
    }));
    cerrarModal('modal-horario');
    toast('✅ Bloque de horario agregado');
    await cargarHorarios();
  } catch (e) {
    errorModal('mh-err', e.message);
  } finally {
    boton.disabled = false;
  }
}

async function borrarHorario(id) {
  if (!confirm('¿Eliminar este bloque de horario?')) return;
  try {
    await conSesion(apiDentista.eliminarHorario(id));
    toast('🗑 Bloque eliminado');
    await cargarHorarios();
  } catch (e) {
    toast(`❌ ${e.message}`);
  }
}

/* ---------- Servicios ---------- */

function pintarServicios() {
  $('conteo-servicios').textContent = `${estado.servicios.length} registrados`;
  if (estado.servicios.length === 0) {
    $('tabla-servicios').innerHTML = '<p class="vacio">No hay servicios registrados.</p>';
    return;
  }
  $('tabla-servicios').innerHTML = `
    <table class="srv-tbl">
      <thead><tr><th>Servicio</th><th>Costo</th><th>Acciones</th></tr></thead>
      <tbody>
        ${estado.servicios.map((s) => `
          <tr class="${s.activo ? '' : 'srv-off'}">
            <td><span class="srv-dot"></span>${escapar(s.nombre)}${s.activo ? '' : ' <span class="badge br">Oculto</span>'}
              ${s.descripcion ? `<div style="font-size:11px;color:var(--text-muted);margin-left:14px;">${escapar(s.descripcion)}</div>` : ''}</td>
            <td class="srv-price">${precio(s.costo)}</td>
            <td><div class="srv-acts">
              <button class="btn btn-outline" style="padding:3px 8px;font-size:10px;" data-editar-servicio="${s.id}">Editar</button>
              <button class="btn btn-danger" style="padding:3px 8px;font-size:10px;" data-borrar-servicio="${s.id}">Eliminar</button>
            </div></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
}

function abrirServicio(servicio) {
  estado.servicioEnEdicion = servicio;
  $('ms-titulo').textContent = servicio ? 'Editar servicio' : 'Nuevo servicio';
  $('mn').value = servicio?.nombre ?? '';
  $('md').value = servicio?.descripcion ?? '';
  $('mc').value = servicio?.costo ?? '';
  $('ma').checked = servicio?.activo ?? true;
  errorModal('ms-err', '');
  abrirModal('modal-servicio');
  $('mn').focus();
}

async function guardarServicio() {
  errorModal('ms-err', '');
  const datos = {
    nombre: $('mn').value.trim(),
    descripcion: $('md').value.trim(),
    costo: $('mc').value === '' ? null : Number($('mc').value),
    activo: $('ma').checked,
  };
  const boton = $('ms-guardar');
  boton.disabled = true;
  try {
    if (estado.servicioEnEdicion) {
      await conSesion(apiDentista.actualizarServicio(estado.servicioEnEdicion.id, datos));
      toast('✅ Servicio actualizado');
    } else {
      await conSesion(apiDentista.crearServicio(datos));
      toast('✅ Servicio agregado correctamente');
    }
    cerrarModal('modal-servicio');
    await cargarServicios();
  } catch (e) {
    errorModal('ms-err', e.message);
  } finally {
    boton.disabled = false;
  }
}

async function borrarServicio(id) {
  const servicio = estado.servicios.find((s) => s.id === id);
  if (!confirm(`¿Eliminar "${servicio?.nombre ?? 'este servicio'}"? Dejará de aparecer en el catálogo.`)) return;
  try {
    await conSesion(apiDentista.eliminarServicio(id));
    toast('🗑 Servicio eliminado');
    await cargarServicios();
  } catch (e) {
    toast(`❌ ${e.message}`);
  }
}

/* ---------- Eventos ---------- */

document.addEventListener('click', (e) => {
  const item = e.target.closest('[data-scr]');
  if (item) mostrarPantalla(item.dataset.scr);

  if (e.target.closest('[data-menu]')) $('sidebar').classList.toggle('open');

  const responder = e.target.closest('[data-responder]');
  if (responder) responderCita(Number(responder.dataset.responder), responder.dataset.estatus, responder);

  const dia = e.target.closest('.cd[data-fecha]');
  if (dia) mostrarDia(dia.dataset.fecha);

  const borrarH = e.target.closest('[data-borrar-horario]');
  if (borrarH) borrarHorario(Number(borrarH.dataset.borrarHorario));

  const editarS = e.target.closest('[data-editar-servicio]');
  if (editarS) abrirServicio(estado.servicios.find((s) => s.id === Number(editarS.dataset.editarServicio)));

  const borrarS = e.target.closest('[data-borrar-servicio]');
  if (borrarS) borrarServicio(Number(borrarS.dataset.borrarServicio));

  const cerrar = e.target.closest('[data-cerrar]');
  if (cerrar) cerrarModal(cerrar.dataset.cerrar);

  if (e.target.classList.contains('modal-ov')) cerrarModal(e.target.id);
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') document.querySelectorAll('.modal-ov.open').forEach((m) => m.classList.remove('open'));
});

$('btn-logout').addEventListener('click', salir);
$('filtro-estatus').addEventListener('change', pintarSolicitudes);
$('cal-prev').addEventListener('click', () => { calMes -= 1; if (calMes < 0) { calMes = 11; calAnio -= 1; } pintarCalendario(); });
$('cal-next').addEventListener('click', () => { calMes += 1; if (calMes > 11) { calMes = 0; calAnio += 1; } pintarCalendario(); });
$('btn-nuevo-servicio').addEventListener('click', () => abrirServicio(null));
$('ms-guardar').addEventListener('click', guardarServicio);
$('btn-nuevo-horario').addEventListener('click', () => {
  const manana = new Date(Date.now() + 86400000);
  $('mh-fecha').value = `${manana.getFullYear()}-${String(manana.getMonth() + 1).padStart(2, '0')}-${String(manana.getDate()).padStart(2, '0')}`;
  $('mh-fecha').min = hoyIso();
  errorModal('mh-err', '');
  abrirModal('modal-horario');
});
$('mh-guardar').addEventListener('click', guardarHorario);
$('btn-qr-actualizar').addEventListener('click', () => {
  $('qr-img').src = `/api/qr?v=${Date.now()}`;
  toast('🔄 QR actualizado');
});

/* ---------- Inicio ---------- */

async function iniciar() {
  if (!sesion.token()) {
    salir();
    return;
  }
  try {
    const { usuario } = await conSesion(apiAuth.verificar());
    $('sb-name').textContent = usuario.nombre || sesion.nombre();
    $('sb-av').textContent = iniciales(usuario.nombre || sesion.nombre()) || 'DR';
  } catch {
    return;
  }
  const f = new Date();
  $('hoy-texto').textContent = `${f.getDate()} de ${MESES[f.getMonth()].toLowerCase()} de ${f.getFullYear()}`;
  $('qr-url').textContent = `${location.origin}/`;

  // Servicios primero para mostrar el costo en las solicitudes.
  await Promise.allSettled([cargarServicios(), cargarHorarios()]);
  await cargarCitas().catch((e) => toast(`❌ ${e.message}`));
}

iniciar();
