/** auth-service — raíz de composición: arma dependencias y levanta el servidor. */
const { crearApp } = require('./app');
const { ServicioAutenticacion } = require('./application/autenticacion');
const {
  crearPool, MysqlDentistaRepository, bcryptHasher, crearTokens, crearNotificador,
} = require('./infrastructure/adaptadores');

const auth = new ServicioAutenticacion({
  dentistas: new MysqlDentistaRepository(crearPool()),
  hasher: bcryptHasher,
  tokens: crearTokens(process.env.JWT_SECRET, process.env.JWT_EXPIRA || '8h'),
  notificador: crearNotificador(process.env.NOTIF_URL || 'http://notif-service:3001', process.env.INTERNAL_KEY || ''),
});

const puerto = Number(process.env.PORT || 3000);
crearApp(auth).listen(puerto, () => console.log(`auth-service escuchando en el puerto ${puerto}`));
