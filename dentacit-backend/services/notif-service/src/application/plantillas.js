/**
 * Plantillas de correo (capa de aplicación): arman asunto y contenido
 * a partir de los datos de la cita. No dependen de cómo se envía el correo.
 */

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio',
  'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

/** Escapa texto para insertarlo en HTML sin riesgo de inyección. */
function escapar(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** "2026-11-20" → "20 de noviembre de 2026" */
function fechaLarga(fecha) {
  const [a, m, d] = String(fecha).split('-').map(Number);
  if (!a || !m || !d) return String(fecha);
  return `${d} de ${MESES[m - 1]} de ${a}`;
}

function marco(titulo, color, cuerpoHtml) {
  return `<!doctype html><html><body style="margin:0;background:#F1F3F8;font-family:Arial,sans-serif">
<table width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:24px">
<table width="560" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;overflow:hidden">
<tr><td style="background:#0F2340;color:#fff;padding:20px 28px;font-size:22px;font-weight:bold">DentaCit</td></tr>
<tr><td style="padding:28px;color:#0F2340;font-size:15px;line-height:1.5">
<h2 style="margin:0 0 16px;color:${color}">${escapar(titulo)}</h2>${cuerpoHtml}</td></tr>
<tr><td style="padding:16px 28px;background:#F1F3F8;color:#6B7280;font-size:12px">
Este es un mensaje automático de DentaCit. No respondas a este correo.</td></tr>
</table></td></tr></table></body></html>`;
}

/**
 * Correo para el paciente cuando el dentista responde su solicitud (RF-06).
 * @param {{estatus:string, paciente_nombre:string, servicio:string, fecha:string, hora:string}} cita
 * @returns {{asunto:string, html:string, texto:string}}
 */
function correoRespuestaCita(cita) {
  const nombre = escapar(cita.paciente_nombre);
  const servicio = escapar(cita.servicio);
  const fecha = fechaLarga(cita.fecha);
  const hora = escapar(cita.hora);

  if (cita.estatus === 'Aceptada') {
    return {
      asunto: `Tu cita fue confirmada — ${fecha}, ${cita.hora}`,
      texto: `Hola ${cita.paciente_nombre}: tu cita de ${cita.servicio} quedó confirmada para el ${fecha} a las ${cita.hora}. `
        + 'Llega 10 minutos antes, trae una identificación y, si tienes estudios previos, llévalos contigo.',
      html: marco('¡Tu cita está confirmada!', '#16A34A', `
<p>Hola <b>${nombre}</b>, el dentista aceptó tu solicitud.</p>
<table cellpadding="6" style="background:#F1F3F8;border-radius:8px;width:100%">
<tr><td><b>Servicio</b></td><td>${servicio}</td></tr>
<tr><td><b>Fecha</b></td><td>${escapar(fecha)}</td></tr>
<tr><td><b>Hora</b></td><td>${hora}</td></tr></table>
<p><b>Recomendaciones:</b></p>
<ul><li>Llega 10 minutos antes.</li><li>Trae una identificación.</li>
<li>Si tienes radiografías o estudios previos, llévalos.</li>
<li>Si no puedes asistir, avisa con anticipación.</li></ul>`),
    };
  }
  return {
    asunto: 'Tu solicitud de cita no pudo ser aceptada',
    texto: `Hola ${cita.paciente_nombre}: por ahora no fue posible atender tu solicitud de ${cita.servicio} `
      + `para el ${fecha} a las ${cita.hora}. Puedes solicitar otro horario desde el código QR del consultorio.`,
    html: marco('Tu solicitud no pudo ser aceptada', '#DC2626', `
<p>Hola <b>${nombre}</b>, por ahora no fue posible atender tu solicitud de <b>${servicio}</b>
para el <b>${escapar(fecha)}</b> a las <b>${hora}</b>.</p>
<p>Puedes elegir otro horario disponible escaneando el código QR del consultorio.</p>`),
  };
}

/**
 * Correo de recuperación de contraseña del dentista.
 * @param {{nombre:string, token:string, url?:string}} datos
 */
function correoRecuperacion({ nombre, token, url }) {
  const enlace = url ? `${url}?token=${encodeURIComponent(token)}` : null;
  return {
    asunto: 'Recupera tu contraseña de DentaCit',
    texto: `Hola ${nombre}: tu código para restablecer la contraseña es ${token}. Vence en 30 minutos.`,
    html: marco('Recupera tu contraseña', '#2563EB', `
<p>Hola <b>${escapar(nombre)}</b>, recibimos una solicitud para restablecer tu contraseña.</p>
<p>Tu código es:</p><p style="font-size:18px;font-family:monospace;background:#F1F3F8;padding:12px;border-radius:8px">${escapar(token)}</p>
${enlace ? `<p><a href="${escapar(enlace)}">Restablecer contraseña</a></p>` : ''}
<p>El código vence en 30 minutos. Si no fuiste tú, ignora este correo.</p>`),
  };
}

module.exports = { correoRespuestaCita, correoRecuperacion, fechaLarga, escapar };
