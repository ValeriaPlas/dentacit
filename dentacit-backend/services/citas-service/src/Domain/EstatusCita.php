<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/**
 * Estados del ciclo de vida de una cita.
 * Transiciones válidas: Pendiente → Aceptada | Rechazada.
 */
enum EstatusCita: string
{
    case Pendiente = 'Pendiente';
    case Aceptada = 'Aceptada';
    case Rechazada = 'Rechazada';

    /**
     * Convierte el texto recibido en la petición a un estado.
     *
     * @throws ReglaNegocioException si el texto no es un estado válido.
     */
    public static function desdeTexto(string $texto): self
    {
        foreach (self::cases() as $caso) {
            if (strcasecmp($caso->value, trim($texto)) === 0) {
                return $caso;
            }
        }
        throw new ReglaNegocioException("Estatus inválido: \"$texto\".");
    }
}
