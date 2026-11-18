/**
 * Cliente del API Gateway de DentaCit.
 * Todas las llamadas usan rutas relativas (/api/...) porque el gateway sirve
 * también esta página; así no hay problemas de CORS.
 */

/** Error con el mensaje que devuelve el servidor en {"ok":false,"msg":"..."}. */
export class ErrorApi extends Error {
  constructor(mensaje, estado) {
    super(mensaje);
    this.estado = estado;
  }
}

async function solicitar(ruta, opciones = {}) {
  let respuesta;
  try {
    respuesta = await fetch(`/api/${ruta}`, {
      ...opciones,
      headers: { Accept: 'application/json', ...(opciones.body ? { 'Content-Type': 'application/json' } : {}) },
    });
  } catch {
    throw new ErrorApi('No pudimos conectarnos con el consultorio. Revisa tu conexión e inténtalo de nuevo.', 0);
  }
  const cuerpo = respuesta.status === 204 ? null : await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    throw new ErrorApi(cuerpo?.msg || 'Ocurrió un error. Inténtalo de nuevo.', respuesta.status);
  }
  return cuerpo;
}

export const api = {
  /** Servicios activos del catálogo. */
  servicios: () => solicitar('servicios'),

  /** Próximos días con horas libres: [{fecha, horas:[]}]. */
  proximosDias: () => solicitar('horarios/proximos'),

  /** Horas libres de un día. */
  horasDisponibles: (fecha) => solicitar(`horarios/disponibles?fecha=${encodeURIComponent(fecha)}`),

  /** Envía la solicitud de cita. */
  solicitarCita: (datos) => solicitar('citas', { method: 'POST', body: JSON.stringify(datos) }),

  /** Citas del paciente. */
  misCitas: (correo) => solicitar(`citas/paciente?correo=${encodeURIComponent(correo)}`),
};
