/**
 * Cliente del API Gateway de DentaCit (compartido por la vista del paciente y el panel del dentista).
 * Todas las llamadas usan rutas relativas (/api/...) porque el gateway sirve también estas páginas.
 */

/** Error con el mensaje que devuelve el servidor en {"ok":false,"msg":"..."}. */
export class ErrorApi extends Error {
  constructor(mensaje, estado) {
    super(mensaje);
    this.estado = estado;
  }
}

/* ---------- Sesión del dentista (token JWT) ---------- */

const CLAVE_TOKEN = 'dentacit-token';
const CLAVE_NOMBRE = 'dentacit-dentista';

export const sesion = {
  token() {
    try {
      return sessionStorage.getItem(CLAVE_TOKEN) || localStorage.getItem(CLAVE_TOKEN);
    } catch {
      return null;
    }
  },
  nombre() {
    try {
      return sessionStorage.getItem(CLAVE_NOMBRE) || localStorage.getItem(CLAVE_NOMBRE) || 'Dentista';
    } catch {
      return 'Dentista';
    }
  },
  /** @param {boolean} recordar true = localStorage (persiste), false = solo esta pestaña. */
  guardar(token, nombre, recordar) {
    try {
      const almacen = recordar ? localStorage : sessionStorage;
      almacen.setItem(CLAVE_TOKEN, token);
      almacen.setItem(CLAVE_NOMBRE, nombre);
    } catch { /* sin almacenamiento disponible */ }
  },
  cerrar() {
    try {
      [sessionStorage, localStorage].forEach((a) => { a.removeItem(CLAVE_TOKEN); a.removeItem(CLAVE_NOMBRE); });
    } catch { /* nada que limpiar */ }
  },
};

/* ---------- Petición base ---------- */

async function solicitar(ruta, { metodo = 'GET', cuerpo, conToken = false } = {}) {
  const encabezados = { Accept: 'application/json' };
  if (cuerpo !== undefined) encabezados['Content-Type'] = 'application/json';
  if (conToken && sesion.token()) encabezados.Authorization = `Bearer ${sesion.token()}`;

  let respuesta;
  try {
    respuesta = await fetch(`/api/${ruta}`, {
      method: metodo,
      headers: encabezados,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });
  } catch {
    throw new ErrorApi('No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', 0);
  }
  const datos = respuesta.status === 204 ? null : await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    throw new ErrorApi(datos?.msg || 'Ocurrió un error. Inténtalo de nuevo.', respuesta.status);
  }
  return datos;
}

/* ---------- Endpoints públicos (paciente) ---------- */

export const apiPaciente = {
  servicios: () => solicitar('servicios'),
  proximosDias: () => solicitar('horarios/proximos'),
  /** {fecha, horas:[...], bloques:[{hora, libre}]} */
  bloquesDelDia: (fecha) => solicitar(`horarios/disponibles?fecha=${encodeURIComponent(fecha)}`),
  solicitarCita: (datos) => solicitar('citas', { metodo: 'POST', cuerpo: datos }),
  misCitas: (correo) => solicitar(`citas/paciente?correo=${encodeURIComponent(correo)}`),
};

/* ---------- Autenticación ---------- */

export const apiAuth = {
  login: (correo, password) => solicitar('auth/login', { metodo: 'POST', cuerpo: { correo, password } }),
  verificar: () => solicitar('auth/verificar', { conToken: true }),
  recuperar: (correo) => solicitar('auth/recuperar', { metodo: 'POST', cuerpo: { correo } }),
  restablecer: (token, password) => solicitar('auth/restablecer', { metodo: 'POST', cuerpo: { token, password } }),
};

/* ---------- Endpoints del dentista (requieren token) ---------- */

export const apiDentista = {
  citas: (estatus) => solicitar(`citas${estatus ? `?estatus=${encodeURIComponent(estatus)}` : ''}`, { conToken: true }),
  responder: (id, estatus) => solicitar(`citas/${id}/estatus`, { metodo: 'PATCH', cuerpo: { estatus }, conToken: true }),
  horarios: () => solicitar('horarios', { conToken: true }),
  crearHorario: (datos) => solicitar('horarios', { metodo: 'POST', cuerpo: datos, conToken: true }),
  eliminarHorario: (id) => solicitar(`horarios/${id}`, { metodo: 'DELETE', conToken: true }),
  servicios: () => solicitar('servicios', { conToken: true }),
  crearServicio: (datos) => solicitar('servicios', { metodo: 'POST', cuerpo: datos, conToken: true }),
  actualizarServicio: (id, datos) => solicitar(`servicios/${id}`, { metodo: 'PUT', cuerpo: datos, conToken: true }),
  eliminarServicio: (id) => solicitar(`servicios/${id}`, { metodo: 'DELETE', conToken: true }),
};

/* ---------- Utilidades compartidas ---------- */

/** Escapa texto antes de insertarlo como HTML (evita XSS). */
export function escapar(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

/** 600 → "$600" (sin decimales, como en el diseño original). */
export const precio = (n) => `$${Number(n).toLocaleString('es-MX', { maximumFractionDigits: 0 })}`;

/** "09:00" → "9:00 am", "15:00" → "3:00 pm". */
export function hora12(hhmm) {
  const [h, m] = String(hhmm).split(':').map(Number);
  const sufijo = h >= 12 ? 'pm' : 'am';
  return `${((h + 11) % 12) + 1}:${String(m).padStart(2, '0')} ${sufijo}`;
}

const MESES_CORTOS = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
export const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto',
  'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

/** "2026-11-20" → { dia: "20", mes: "Nov" } */
export function partesFecha(iso) {
  const [, m, d] = String(iso).split('-').map(Number);
  return { dia: String(d).padStart(2, '0'), mes: MESES_CORTOS[m - 1] ?? '' };
}

/** "2026-11-20" → "20/11/2026" */
export const fechaCorta = (iso) => String(iso).split('-').reverse().join('/');

/** "2026-11-20" → "20 de noviembre" */
export function fechaLarga(iso) {
  const [, m, d] = String(iso).split('-').map(Number);
  return `${d} de ${MESES[m - 1]?.toLowerCase() ?? ''}`;
}

/** Fecha local de hoy en AAAA-MM-DD. */
export function hoyIso() {
  const h = new Date();
  return `${h.getFullYear()}-${String(h.getMonth() + 1).padStart(2, '0')}-${String(h.getDate()).padStart(2, '0')}`;
}

/** Iniciales de un nombre: "Ana López" → "AL". */
export function iniciales(nombre) {
  const partes = String(nombre || '').trim().split(/\s+/).filter(Boolean);
  return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase();
}
