/**
 * Errores de dominio del servicio de autenticación.
 * La capa HTTP los traduce a códigos de estado.
 */

/** Violación de una regla de negocio (HTTP 422). */
class ReglaNegocioError extends Error {}

/** Credenciales o token inválidos (HTTP 401). */
class NoAutorizadoError extends Error {}

module.exports = { ReglaNegocioError, NoAutorizadoError };
