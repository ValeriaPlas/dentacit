/** Pruebas de notif-service: plantillas y endpoints (correo simulado, sin SMTP). */
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { correoRespuestaCita, fechaLarga, escapar } = require('../src/application/plantillas');
const { crearApp } = require('../src/app');

const cita = {
  estatus: 'Aceptada', paciente_nombre: 'Ana López', paciente_correo: 'ana@correo.com',
  servicio: 'Limpieza dental', fecha: '2026-11-20', hora: '10:00',
};

test('fechaLarga da formato en español', () => {
  assert.equal(fechaLarga('2026-11-20'), '20 de noviembre de 2026');
});

test('correo de cita aceptada incluye datos y recomendaciones', () => {
  const c = correoRespuestaCita(cita);
  assert.match(c.asunto, /confirmada/);
  assert.match(c.html, /Limpieza dental/);
  assert.match(c.html, /Recomendaciones/);
});

test('correo de cita rechazada usa otra plantilla', () => {
  const c = correoRespuestaCita({ ...cita, estatus: 'Rechazada' });
  assert.match(c.asunto, /no pudo ser aceptada/);
});

test('los datos del paciente se escapan para evitar inyección de HTML', () => {
  const c = correoRespuestaCita({ ...cita, paciente_nombre: '<script>alert(1)</script>' });
  assert.ok(!c.html.includes('<script>'));
  assert.equal(escapar('<b>'), '&lt;b&gt;');
});

async function conServidor(fn) {
  const enviados = [];
  const app = crearApp({ enviar: async (m) => { enviados.push(m); return 'id-1'; } }, {
    llaveInterna: 'llave', urlWeb: 'http://localhost:8080/',
  });
  const servidor = app.listen(0);
  const url = `http://127.0.0.1:${servidor.address().port}`;
  try {
    await fn(url, enviados);
  } finally {
    servidor.close();
  }
}

test('POST /api/notificaciones/cita sin llave interna responde 403', () => conServidor(async (url, enviados) => {
  const r = await fetch(`${url}/api/notificaciones/cita`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(cita),
  });
  assert.equal(r.status, 403);
  assert.equal(enviados.length, 0);
}));

test('POST /api/notificaciones/cita con llave envía el correo al paciente', () => conServidor(async (url, enviados) => {
  const r = await fetch(`${url}/api/notificaciones/cita`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Internal-Key': 'llave' }, body: JSON.stringify(cita),
  });
  assert.equal(r.status, 200);
  assert.equal(enviados[0].para, 'ana@correo.com');
}));

test('GET /api/qr devuelve una imagen PNG', () => conServidor(async (url) => {
  const r = await fetch(`${url}/api/qr`);
  assert.equal(r.headers.get('content-type'), 'image/png');
  const bytes = Buffer.from(await r.arrayBuffer());
  assert.equal(bytes.subarray(1, 4).toString(), 'PNG');
}));
