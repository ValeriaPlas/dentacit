<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/** Se lanza cuando el recurso solicitado no existe (HTTP 404). */
final class NoEncontradoException extends \RuntimeException
{
}
