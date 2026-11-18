const crypto = require('node:crypto');
const { ReglaNegocioError, NoAutorizadoError } = require('../domain/errores');

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MINUTOS_RECUPERACION = 30;

/**
 * Casos de uso de autenticación del dentista (RF-01).
 * No depende de Express ni de MySQL: recibe sus puertos por constructor.
 */
class ServicioAutenticacion {
  /**
   * @param {object} deps
   * @param {object} deps.dentistas  Repositorio: buscarPorCorreo, buscarPorId, actualizarPassword,
   *                                 guardarRecuperacion, buscarRecuperacion, borrarRecuperaciones
   * @param {object} deps.hasher     comparar(texto, hash), generar(texto)
   * @param {object} deps.tokens     firmar(payload), verificar(token)
   * @param {object} deps.notificador enviarRecuperacion({correo, nombre, token})
   */
  constructor({ dentistas, hasher, tokens, notificador }) {
    this.dentistas = dentistas;
    this.hasher = hasher;
    this.tokens = tokens;
    this.notificador = notificador;
  }

  /** Valida credenciales y emite un JWT. */
  async iniciarSesion(correo, password) {
    const correoLimpio = String(correo || '').trim().toLowerCase();
    if (!correoLimpio || !password) {
      throw new ReglaNegocioError('Ingresa tu correo y tu contraseña.');
    }
    if (!CORREO_VALIDO.test(correoLimpio)) {
      throw new ReglaNegocioError('El correo no tiene un formato válido.');
    }
    const dentista = await this.dentistas.buscarPorCorreo(correoLimpio);
    // Mismo mensaje si no existe o si la contraseña es incorrecta (no revelar qué falló).
    if (!dentista || !(await this.hasher.comparar(String(password), dentista.password_hash))) {
      throw new NoAutorizadoError('Correo o contraseña incorrectos.');
    }
    const token = this.tokens.firmar({ sub: dentista.id, nombre: dentista.nombre, rol: 'dentista' });
    return { token, nombre: dentista.nombre };
  }

  /** Devuelve los datos del token si es válido. */
  verificar(token) {
    if (!token) throw new NoAutorizadoError('Falta el token.');
    try {
      return this.tokens.verificar(token);
    } catch {
      throw new NoAutorizadoError('La sesión expiró o el token no es válido.');
    }
  }

  /**
   * Inicia la recuperación de contraseña. Siempre responde igual para no revelar
   * qué correos están registrados.
   */
  async solicitarRecuperacion(correo) {
    const correoLimpio = String(correo || '').trim().toLowerCase();
    if (!CORREO_VALIDO.test(correoLimpio)) {
      throw new ReglaNegocioError('Indica un correo válido.');
    }
    const dentista = await this.dentistas.buscarPorCorreo(correoLimpio);
    if (dentista) {
      const token = crypto.randomBytes(24).toString('hex');
      const expira = new Date(Date.now() + MINUTOS_RECUPERACION * 60 * 1000);
      await this.dentistas.guardarRecuperacion(dentista.id, sha256(token), expira);
      await this.notificador.enviarRecuperacion({ correo: dentista.correo, nombre: dentista.nombre, token });
    }
    return { msg: 'Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.' };
  }

  /** Cambia la contraseña usando el código recibido por correo. */
  async restablecer(token, nuevaPassword) {
    if (!token) throw new ReglaNegocioError('Falta el código de recuperación.');
    if (String(nuevaPassword || '').length < 8) {
      throw new ReglaNegocioError('La nueva contraseña debe tener al menos 8 caracteres.');
    }
    const registro = await this.dentistas.buscarRecuperacion(sha256(String(token)));
    if (!registro || new Date(registro.expira) < new Date()) {
      throw new ReglaNegocioError('El código no es válido o ya expiró.');
    }
    await this.dentistas.actualizarPassword(registro.id_dentista, await this.hasher.generar(String(nuevaPassword)));
    await this.dentistas.borrarRecuperaciones(registro.id_dentista);
    return { msg: 'Contraseña actualizada. Ya puedes iniciar sesión.' };
  }
}

function sha256(texto) {
  return crypto.createHash('sha256').update(texto).digest('hex');
}

module.exports = { ServicioAutenticacion, sha256 };
