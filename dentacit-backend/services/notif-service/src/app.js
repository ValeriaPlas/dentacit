const express = require('express');
const QRCode = require('qrcode');
const { correoRespuestaCita, correoRecuperacion } = require('./application/plantillas');

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Construye la aplicación Express de notif-service.
 * @param {{enviar:Function}} correo     Adaptador de envío de correo.
 * @param {{llaveInterna:string, urlWeb:string, urlRecuperacion?:string}} config
 */
function crearApp(correo, config) {
  const app = express();
  app.use(express.json({ limit: '50kb' }));

  /** Solo otros microservicios (con la llave interna) pueden pedir correos. */
  const soloInterno = (req, res, next) => {
    if (!config.llaveInterna || req.get('x-internal-key') !== config.llaveInterna) {
      return res.status(403).json({ ok: false, msg: 'Acceso solo para servicios internos.' });
    }
    return next();
  };

  app.get('/api/notificaciones/salud', (req, res) => res.json({ servicio: 'notif-service', ok: true }));

  /** POST /api/notificaciones/cita — llamado por citas-service al aceptar o rechazar. */
  app.post('/api/notificaciones/cita', soloInterno, async (req, res, next) => {
    try {
      const c = req.body || {};
      if (!['Aceptada', 'Rechazada'].includes(c.estatus) || !CORREO_VALIDO.test(c.paciente_correo || '')) {
        return res.status(422).json({ ok: false, msg: 'Datos de la cita incompletos.' });
      }
      const plantilla = correoRespuestaCita(c);
      const id = await correo.enviar({ para: c.paciente_correo, ...plantilla });
      return res.json({ ok: true, id });
    } catch (e) {
      return next(e);
    }
  });

  /** POST /api/notificaciones/recuperacion — llamado por auth-service. */
  app.post('/api/notificaciones/recuperacion', soloInterno, async (req, res, next) => {
    try {
      const { correo: para, nombre, token } = req.body || {};
      if (!CORREO_VALIDO.test(para || '') || !token) {
        return res.status(422).json({ ok: false, msg: 'Datos incompletos.' });
      }
      const plantilla = correoRecuperacion({ nombre, token, url: config.urlRecuperacion });
      const id = await correo.enviar({ para, ...plantilla });
      return res.json({ ok: true, id });
    } catch (e) {
      return next(e);
    }
  });

  /** GET /api/qr — PNG con el QR que lleva al catálogo del paciente (RF-02). */
  app.get('/api/qr', async (req, res, next) => {
    try {
      const url = config.urlWeb;
      const png = await QRCode.toBuffer(url, { width: Number(req.query.tamano) || 400, margin: 2, color: { dark: '#0F2340' } });
      res.set('Content-Type', 'image/png');
      res.set('Content-Disposition', 'inline; filename="dentacit_qr.png"');
      res.send(png);
    } catch (e) {
      next(e);
    }
  });

  app.use((req, res) => res.status(404).json({ ok: false, msg: 'Ruta no encontrada.' }));
  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    console.error('[notif-service]', err);
    res.status(502).json({ ok: false, msg: 'No se pudo enviar la notificación.' });
  });
  return app;
}

module.exports = { crearApp };
