/** notif-service — raíz de composición. */
const { crearApp } = require('./app');
const { crearCorreo } = require('./infrastructure/correo');

const correo = crearCorreo();
const app = crearApp(correo, {
  llaveInterna: process.env.INTERNAL_KEY || '',
  urlWeb: process.env.WEB_URL || 'http://localhost:8080/',
  urlRecuperacion: process.env.RECUPERACION_URL || '',
});

const puerto = Number(process.env.PORT || 3001);
app.listen(puerto, () => console.log(`notif-service escuchando en el puerto ${puerto} (correo: ${correo.modo})`));
