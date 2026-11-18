const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const mysql = require('mysql2/promise');

/** Pool de conexiones MySQL a partir de variables de entorno. */
function crearPool() {
  return mysql.createPool({
    host: process.env.DB_HOST || '127.0.0.1',
    port: Number(process.env.DB_PORT || 3306),
    database: process.env.DB_NAME || 'dentacit',
    user: process.env.DB_USER || 'dentacit',
    password: process.env.DB_PASS || '',
    waitForConnections: true,
    connectionLimit: 5,
    timezone: '-06:00',
    dateStrings: true,
  });
}

/** Repositorio de dentistas sobre MySQL (consultas parametrizadas). */
class MysqlDentistaRepository {
  constructor(pool) {
    this.pool = pool;
  }

  async buscarPorCorreo(correo) {
    const [filas] = await this.pool.execute('SELECT * FROM dentistas WHERE correo = ?', [correo]);
    return filas[0] || null;
  }

  async actualizarPassword(id, hash) {
    await this.pool.execute('UPDATE dentistas SET password_hash = ? WHERE id = ?', [hash, id]);
  }

  async guardarRecuperacion(idDentista, tokenHash, expira) {
    await this.pool.execute(
      'INSERT INTO password_resets (id_dentista, token_hash, expira) VALUES (?, ?, ?)',
      [idDentista, tokenHash, expira],
    );
  }

  async buscarRecuperacion(tokenHash) {
    const [filas] = await this.pool.execute(
      'SELECT * FROM password_resets WHERE token_hash = ? ORDER BY id DESC LIMIT 1',
      [tokenHash],
    );
    return filas[0] || null;
  }

  async borrarRecuperaciones(idDentista) {
    await this.pool.execute('DELETE FROM password_resets WHERE id_dentista = ?', [idDentista]);
  }
}

/** Hash de contraseñas con bcrypt (RNF-02). */
const bcryptHasher = {
  comparar: (texto, hash) => bcrypt.compare(texto, hash),
  generar: (texto) => bcrypt.hash(texto, 10),
};

/** Emisión y verificación de JWT HS256. */
function crearTokens(secreto, expiracion = '8h') {
  if (!secreto) throw new Error('Falta la variable JWT_SECRET.');
  return {
    firmar: (payload) => jwt.sign(payload, secreto, { algorithm: 'HS256', expiresIn: expiracion }),
    verificar: (token) => jwt.verify(token, secreto, { algorithms: ['HS256'] }),
  };
}

/** Cliente hacia notif-service para el correo de recuperación. */
function crearNotificador(urlBase, llaveInterna) {
  return {
    async enviarRecuperacion(datos) {
      try {
        const r = await fetch(`${urlBase}/api/notificaciones/recuperacion`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Internal-Key': llaveInterna },
          body: JSON.stringify(datos),
        });
        if (!r.ok) console.error('[auth-service] notif-service respondió', r.status);
      } catch (e) {
        console.error('[auth-service] No se pudo contactar a notif-service:', e.message);
      }
    },
  };
}

module.exports = { crearPool, MysqlDentistaRepository, bcryptHasher, crearTokens, crearNotificador };
