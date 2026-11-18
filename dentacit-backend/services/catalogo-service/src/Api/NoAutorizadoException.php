<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Api;

/** Falta el token JWT o no es válido (HTTP 401). */
final class NoAutorizadoException extends \RuntimeException
{
}
