/**
 * Cliente web del paciente — DentaCit.
 * Vistas: catálogo de servicios, formulario de cita y consulta de mis citas.
 * Navegación por hash (#servicios, #agendar, #mis-citas).
 */
import { api } from './api.js';

const $ = (selector) => document.querySelector(selector);
const formatoPrecio = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' });
const formatoDia = new Intl.DateTimeFormat('es-MX', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });
const formatoFecha = new Intl.DateTimeFormat('es-MX', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' });

const estado = {
  servicios: [],
  dias: [],
  diaElegido: null,
  horaElegida: null,
};

/* ---------- Utilidades ---------- */

/** Escapa texto antes de insertarlo como HTML (evita XSS). */
function escapar(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const aFecha = (iso) => new Date(`${iso}T00:00:00Z`);
const mayuscula = (t) => t.charAt(0).toUpperCase() + t.slice(1);
const diaCorto = (iso) => mayuscula(formatoDia.format(aFecha(iso)).replaceAll('.', ''));
const fechaLarga = (iso) => mayuscula(formatoFecha.format(aFecha(iso)));

/** Recuerda los datos del paciente en este navegador (solo como comodidad). */
const memoria = {
  leer() {
    try { return JSON.parse(localStorage.getItem('dentacit-paciente')) || {}; } catch { return {}; }
  },
  guardar(datos) {
    try { localStorage.setItem('dentacit-paciente', JSON.stringify(datos)); } catch { /* sin almacenamiento */ }
  },
};

/* ---------- Navegación ---------- */

const VISTAS = ['servicios', 'agendar', 'mis-citas'];

function mostrarVista() {
  const vista = VISTAS.includes(location.hash.slice(1)) ? location.hash.slice(1) : 'servicios';
  VISTAS.forEach((v) => { $(`#vista-${v}`).hidden = v !== vista; });
  document.querySelectorAll('.menu a').forEach((a) => {
    a.toggleAttribute('aria-current', a.dataset.vista === vista);
  });
  if (vista === 'agendar') prepararFormulario();
  if (vista === 'mis-citas') prepararConsulta();
  window.scrollTo({ top: 0 });
}

/* ---------- Catálogo ---------- */

async function cargarServicios() {
  const contenedor = $('#lista-servicios');
  try {
    estado.servicios = await api.servicios();
    if (estado.servicios.length === 0) {
      contenedor.innerHTML = '<p class="vacio">Por el momento no hay servicios disponibles.</p>';
      return;
    }
    contenedor.innerHTML = estado.servicios.map((s) => `
      <article class="tarjeta servicio">
        <h3>${escapar(s.nombre)}</h3>
        ${s.descripcion ? `<p>${escapar(s.descripcion)}</p>` : ''}
        <div class="servicio-pie">
          <span class="precio">${formatoPrecio.format(s.costo)}</span>
          <a class="boton boton-secundario boton-chico" href="#agendar" data-servicio="${s.id}">Agendar</a>
        </div>
      </article>`).join('');
    llenarSelectServicios();
  } catch (e) {
    contenedor.innerHTML = `<p class="alerta alerta-error">${escapar(e.message)}</p>`;
  }
}

function llenarSelectServicios() {
  const select = $('#campo-servicio');
  const elegido = select.value;
  select.innerHTML = '<option value="">Elige un servicio…</option>' + estado.servicios
    .map((s) => `<option value="${s.id}">${escapar(s.nombre)} — ${formatoPrecio.format(s.costo)}</option>`)
    .join('');
  select.value = elegido;
}

/* ---------- Formulario de cita ---------- */

async function prepararFormulario() {
  $('#formulario-cita').hidden = false;
  $('#confirmacion').hidden = true;

  const guardados = memoria.leer();
  ['nombre', 'correo', 'telefono'].forEach((campo) => {
    const input = $(`#campo-${campo}`);
    if (!input.value && guardados[campo]) input.value = guardados[campo];
  });
  await cargarDias();
}

async function cargarDias() {
  const contenedor = $('#lista-dias');
  contenedor.innerHTML = '<p class="cargando">Buscando días disponibles…</p>';
  try {
    estado.dias = await api.proximosDias();
  } catch (e) {
    contenedor.innerHTML = `<p class="alerta alerta-error">${escapar(e.message)}</p>`;
    return;
  }
  if (estado.dias.length === 0) {
    contenedor.innerHTML = '<p class="vacio">No hay horarios disponibles en los próximos días. Vuelve a intentarlo más tarde.</p>';
    $('#lista-horas').innerHTML = '';
    return;
  }
  if (!estado.dias.some((d) => d.fecha === estado.diaElegido)) {
    estado.diaElegido = estado.dias[0].fecha;
    estado.horaElegida = null;
  }
  pintarDias();
  pintarHoras();
}

function pintarDias() {
  $('#lista-dias').innerHTML = estado.dias.map(({ fecha, horas }) => `
    <button type="button" class="ficha ficha-dia" role="radio" data-dia="${fecha}"
      aria-checked="${fecha === estado.diaElegido}">
      <strong>${escapar(diaCorto(fecha))}</strong>
      <small>${horas.length} ${horas.length === 1 ? 'lugar' : 'lugares'}</small>
    </button>`).join('');
}

function pintarHoras() {
  const dia = estado.dias.find((d) => d.fecha === estado.diaElegido);
  const contenedor = $('#lista-horas');
  if (!dia) {
    contenedor.innerHTML = '<p class="ayuda">Primero elige un día.</p>';
    return;
  }
  contenedor.innerHTML = dia.horas.map((h) => `
    <button type="button" class="ficha" role="radio" data-hora="${h}" aria-checked="${h === estado.horaElegida}">${h}</button>`).join('');
}

function mostrarError(mensaje) {
  const alerta = $('#error-formulario');
  alerta.textContent = mensaje;
  alerta.hidden = !mensaje;
  if (mensaje) alerta.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

async function enviarCita(evento) {
  evento.preventDefault();
  mostrarError('');

  const datos = {
    id_servicio: Number($('#campo-servicio').value),
    fecha: estado.diaElegido,
    hora: estado.horaElegida,
    nombre: $('#campo-nombre').value.trim(),
    correo: $('#campo-correo').value.trim(),
    telefono: $('#campo-telefono').value.trim(),
    sintomas: $('#campo-sintomas').value.trim(),
  };

  if (!datos.id_servicio) return mostrarError('Elige el servicio que necesitas.');
  if (!datos.fecha || !datos.hora) return mostrarError('Elige el día y la hora de tu cita.');
  if (!datos.nombre) return mostrarError('Escribe tu nombre completo.');
  if (!$('#campo-correo').checkValidity() || !datos.correo) return mostrarError('Escribe un correo válido.');

  const boton = $('#boton-enviar');
  boton.disabled = true;
  boton.textContent = 'Enviando…';
  try {
    await api.solicitarCita(datos);
    memoria.guardar({ nombre: datos.nombre, correo: datos.correo, telefono: datos.telefono });
    mostrarConfirmacion(datos);
  } catch (e) {
    mostrarError(e.message);
    if (e.estado === 422) await cargarDias(); // la hora pudo haberse ocupado
  } finally {
    boton.disabled = false;
    boton.textContent = 'Enviar solicitud';
  }
}

function mostrarConfirmacion(datos) {
  const servicio = estado.servicios.find((s) => s.id === datos.id_servicio);
  $('#resumen-cita').innerHTML = `
    <dt>Servicio</dt><dd>${escapar(servicio?.nombre ?? '')}</dd>
    <dt>Fecha</dt><dd>${escapar(fechaLarga(datos.fecha))}</dd>
    <dt>Hora</dt><dd>${escapar(datos.hora)}</dd>
    ${servicio ? `<dt>Costo</dt><dd>${formatoPrecio.format(servicio.costo)}</dd>` : ''}
    <dt>Correo</dt><dd>${escapar(datos.correo)}</dd>`;
  $('#formulario-cita').hidden = true;
  $('#formulario-cita').reset();
  estado.horaElegida = null;
  const confirmacion = $('#confirmacion');
  confirmacion.hidden = false;
  confirmacion.focus();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

/* ---------- Mis citas ---------- */

function prepararConsulta() {
  const correo = memoria.leer().correo;
  const input = $('#consulta-correo');
  if (correo && !input.value) {
    input.value = correo;
    consultarCitas();
  }
}

async function consultarCitas(evento) {
  evento?.preventDefault();
  const input = $('#consulta-correo');
  const contenedor = $('#lista-citas');
  if (!input.value.trim() || !input.checkValidity()) {
    contenedor.innerHTML = '<p class="alerta alerta-error">Escribe un correo válido.</p>';
    return;
  }
  contenedor.innerHTML = '<p class="cargando">Buscando tus citas…</p>';
  try {
    const citas = await api.misCitas(input.value.trim());
    if (citas.length === 0) {
      contenedor.innerHTML = '<p class="vacio">No encontramos citas con ese correo. <a href="#agendar">Agenda una aquí.</a></p>';
      return;
    }
    contenedor.innerHTML = citas.map((c) => `
      <article class="tarjeta cita">
        <div class="cita-encabezado">
          <h3>${escapar(c.servicio)}</h3>
          <span class="estatus estatus-${escapar(c.estatus.toLowerCase())}">${escapar(c.estatus)}</span>
        </div>
        <p>${escapar(fechaLarga(c.fecha))} · ${escapar(c.hora)}</p>
        ${c.sintomas ? `<p class="sintomas">${escapar(c.sintomas)}</p>` : ''}
      </article>`).join('');
  } catch (e) {
    contenedor.innerHTML = `<p class="alerta alerta-error">${escapar(e.message)}</p>`;
  }
}

/* ---------- Eventos ---------- */

document.addEventListener('click', (e) => {
  const enlaceServicio = e.target.closest('[data-servicio]');
  if (enlaceServicio) $('#campo-servicio').value = enlaceServicio.dataset.servicio;

  const fichaDia = e.target.closest('[data-dia]');
  if (fichaDia) {
    estado.diaElegido = fichaDia.dataset.dia;
    estado.horaElegida = null;
    pintarDias();
    pintarHoras();
  }

  const fichaHora = e.target.closest('[data-hora]');
  if (fichaHora) {
    estado.horaElegida = fichaHora.dataset.hora;
    pintarHoras();
  }
});

$('#formulario-cita').addEventListener('submit', enviarCita);
$('#formulario-consulta').addEventListener('submit', consultarCitas);
window.addEventListener('hashchange', mostrarVista);

cargarServicios();
mostrarVista();
