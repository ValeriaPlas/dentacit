<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Domain;

/**
 * Se lanza cuando una operación viola una regla de negocio del dominio.
 * La capa API la traduce a HTTP 422 con el mensaje para el usuario.
 */
final class ReglaNegocioException extends \DomainException
{
}
