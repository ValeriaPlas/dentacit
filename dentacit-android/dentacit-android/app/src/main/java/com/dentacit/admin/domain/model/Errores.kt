package com.dentacit.admin.domain.model

/**
 * Excepción base del sistema DentaCit.
 *
 * Todas las capas convierten sus errores técnicos en una subclase de esta excepción,
 * de modo que la capa de presentación solo necesita mostrar [message] al usuario.
 *
 * @param mensaje texto legible para el dentista.
 */
open class DentaCitException(mensaje: String) : Exception(mensaje)

/** Se lanza cuando una operación viola una regla de negocio del dominio. */
class ReglaNegocioException(mensaje: String) : DentaCitException(mensaje)

/** Se lanza cuando el servidor rechaza las credenciales o el token JWT expiró. */
class NoAutorizadoException :
    DentaCitException("Sesión expirada o credenciales incorrectas.")

/** Se lanza cuando no hay conexión con el API Gateway. */
class ErrorConexionException :
    DentaCitException("No se pudo conectar con el servidor. Revisa tu conexión.")

/**
 * Se lanza cuando el servidor responde con un error distinto de autenticación.
 *
 * @param mensaje mensaje devuelto por el servicio o uno genérico.
 */
class ErrorServidorException(mensaje: String) : DentaCitException(mensaje)
