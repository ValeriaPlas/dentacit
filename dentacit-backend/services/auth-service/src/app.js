const express = require('express');
const { ReglaNegocioError, NoAutorizadoError } = require('./domain/errores');

/**
 * Construye la aplicación Express (capa API).
 * @param {import('./application/autenticacion').ServicioAutenticacion} auth
 */
function crearApp(auth) {
  const app = express();
  app.use(express.json({ limit: '50kb' }));

  const manejar = (fn) => async (req, res, next) => {
    try {
      await fn(req, res);
    } catch (e) {
      next(e);
    }
  };

  app.get('/api/auth/salud', (req, res) => res.json({ servicio: 'auth-service', ok: true }));

  app.post('/api/auth/login', manejar(async (req, res) => {
    res.json(await auth.iniciarSesion(req.body?.correo, req.body?.password));
  }));

  app.get('/api/auth/verificar', manejar(async (req, res) => {
    const token = (req.get('authorization') || '').replace(/^Bearer\s+/i, '');
    const datos = auth.verificar(token);
    res.json({ ok: true, usuario: { id: datos.sub, nombre: datos.nombre, rol: datos.rol } });
  }));

  app.post('/api/auth/recuperar', manejar(async (req, res) => {
    res.json({ ok: true, ...(await auth.solicitarRecuperacion(req.body?.correo)) });
  }));

  app.post('/api/auth/restablecer', manejar(async (req, res) => {
    res.json({ ok: true, ...(await auth.restablecer(req.body?.token, req.body?.password)) });
  }));

  app.use((req, res) => res.status(404).json({ ok: false, msg: 'Ruta no encontrada.' }));

  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    if (err instanceof ReglaNegocioError) return res.status(422).json({ ok: false, msg: err.message });
    if (err instanceof NoAutorizadoError) return res.status(401).json({ ok: false, msg: err.message });
    if (err.type === 'entity.parse.failed') return res.status(400).json({ ok: false, msg: 'JSON inválido.' });
    console.error('[auth-service]', err);
    return res.status(500).json({ ok: false, msg: 'Error interno del servidor.' });
  });

  return app;
}

module.exports = { crearApp };
