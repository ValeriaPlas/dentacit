/** Pruebas unitarias de los casos de uso de autenticación (node:test, sin base de datos). */
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { ServicioAutenticacion, sha256 } = require('../src/application/autenticacion');
const { ReglaNegocioError, NoAutorizadoError } = require('../src/domain/errores');
const { crearTokens } = require('../src/infrastructure/adaptadores');

function crearServicio() {
  const dentista = { id: 1, nombre: 'Dra. Prueba', correo: 'dra@dentacit.com', password_hash: 'hash:secreta123' };
  const recuperaciones = [];
  const correos = [];
  const repo = {
    buscarPorCorreo: async (c) => (c === dentista.correo ? dentista : null),
    actualizarPassword: async (id, hash) => { dentista.password_hash = hash; },
    guardarRecuperacion: async (id, tokenHash, expira) => recuperaciones.push({ id_dentista: id, token_hash: tokenHash, expira }),
    buscarRecuperacion: async (h) => recuperaciones.find((r) => r.token_hash === h) || null,
    borrarRecuperaciones: async () => { recuperaciones.length = 0; },
  };
  const hasher = {
    comparar: async (texto, hash) => hash === `hash:${texto}`,
    generar: async (texto) => `hash:${texto}`,
  };
  const servicio = new ServicioAutenticacion({
    dentistas: repo,
    hasher,
    tokens: crearTokens('secreto-de-prueba'),
    notificador: { enviarRecuperacion: async (d) => correos.push(d) },
  });
  return { servicio, dentista, recuperaciones, correos };
}

test('login correcto devuelve un token con el id del dentista', async () => {
  const { servicio } = crearServicio();
  const r = await servicio.iniciarSesion('DRA@dentacit.com ', 'secreta123');
  assert.equal(r.nombre, 'Dra. Prueba');
  assert.equal(servicio.verificar(r.token).sub, 1);
});

test('contraseña incorrecta lanza NoAutorizadoError', async () => {
  const { servicio } = crearServicio();
  await assert.rejects(servicio.iniciarSesion('dra@dentacit.com', 'otra'), NoAutorizadoError);
});

test('correo inexistente da el mismo error que contraseña incorrecta', async () => {
  const { servicio } = crearServicio();
  await assert.rejects(servicio.iniciarSesion('nadie@dentacit.com', 'x'), { message: 'Correo o contraseña incorrectos.' });
});

test('campos vacíos o correo mal formado lanzan ReglaNegocioError', async () => {
  const { servicio } = crearServicio();
  await assert.rejects(servicio.iniciarSesion('', ''), ReglaNegocioError);
  await assert.rejects(servicio.iniciarSesion('sin-arroba', 'x'), ReglaNegocioError);
});

test('un token alterado no se acepta', async () => {
  const { servicio } = crearServicio();
  const { token } = await servicio.iniciarSesion('dra@dentacit.com', 'secreta123');
  assert.throws(() => servicio.verificar(`${token}x`), NoAutorizadoError);
});

test('recuperación: envía correo y permite restablecer la contraseña una sola vez', async () => {
  const { servicio, correos, dentista } = crearServicio();
  await servicio.solicitarRecuperacion('dra@dentacit.com');
  assert.equal(correos.length, 1);
  const { token } = correos[0];

  await servicio.restablecer(token, 'nuevaClave2026');
  assert.equal(dentista.password_hash, 'hash:nuevaClave2026');
  await assert.rejects(servicio.restablecer(token, 'otraClave2026'), ReglaNegocioError);
});

test('recuperación de un correo no registrado no envía nada pero responde igual', async () => {
  const { servicio, correos } = crearServicio();
  const r = await servicio.solicitarRecuperacion('nadie@dentacit.com');
  assert.match(r.msg, /Si el correo está registrado/);
  assert.equal(correos.length, 0);
});

test('la nueva contraseña debe tener al menos 8 caracteres', async () => {
  const { servicio, recuperaciones } = crearServicio();
  recuperaciones.push({ id_dentista: 1, token_hash: sha256('abc'), expira: new Date(Date.now() + 60000) });
  await assert.rejects(servicio.restablecer('abc', 'corta'), ReglaNegocioError);
});
