const nodemailer = require('nodemailer');

/**
 * Crea el transporte de correo.
 * Si SMTP_HOST no está configurado, usa un transporte de prueba que solo
 * registra el correo en la consola (útil en desarrollo y en la demo).
 */
function crearCorreo(env = process.env) {
  const remitente = env.SMTP_FROM || 'DentaCit <no-reply@dentacit.local>';
  const transporte = env.SMTP_HOST
    ? nodemailer.createTransport({
      host: env.SMTP_HOST,
      port: Number(env.SMTP_PORT || 587),
      secure: Number(env.SMTP_PORT) === 465,
      auth: env.SMTP_USER ? { user: env.SMTP_USER, pass: env.SMTP_PASS } : undefined,
    })
    : nodemailer.createTransport({ jsonTransport: true });

  return {
    modo: env.SMTP_HOST ? 'smtp' : 'consola',
    async enviar({ para, asunto, html, texto }) {
      const info = await transporte.sendMail({ from: remitente, to: para, subject: asunto, html, text: texto });
      if (!env.SMTP_HOST) {
        console.log(`[notif-service] (modo consola) Correo para ${para}: "${asunto}"`);
      }
      return info.messageId;
    },
  };
}

module.exports = { crearCorreo };
