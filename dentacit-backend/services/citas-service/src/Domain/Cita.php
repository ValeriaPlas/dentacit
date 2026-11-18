<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

use DateTimeImmutable;

/**
 * Entidad de dominio Cita.
 *
 * Protege la regla "una vez aceptada o rechazada, la cita no se modifica"
 * (regla de Integridad de Datos del documento de requerimientos).
 */
final class Cita
{
    /**
     * @param int|null          $id             Identificador; null si aún no se guarda.
     * @param int               $idPaciente     Paciente que la solicita.
     * @param int               $idServicio     Servicio solicitado.
     * @param string            $servicioNombre Nombre del servicio al momento de solicitar.
     * @param DateTimeImmutable $fecha          Día de la cita.
     * @param string            $hora           Hora de inicio en formato HH:MM.
     * @param string            $sintomas       Síntomas descritos por el paciente.
     * @param EstatusCita       $estatus        Estado actual.
     */
    public function __construct(
        public readonly ?int $id,
        public readonly int $idPaciente,
        public readonly int $idServicio,
        public readonly string $servicioNombre,
        public readonly DateTimeImmutable $fecha,
        public readonly string $hora,
        public readonly string $sintomas,
        public readonly EstatusCita $estatus,
    ) {
    }

    /**
     * Crea una nueva solicitud de cita en estado Pendiente (RF-03 / RF-04).
     */
    public static function solicitar(
        int $idPaciente,
        int $idServicio,
        string $servicioNombre,
        DateTimeImmutable $fecha,
        string $hora,
        string $sintomas,
    ): self {
        return new self(null, $idPaciente, $idServicio, $servicioNombre, $fecha, $hora, trim($sintomas), EstatusCita::Pendiente);
    }

    /** Indica si el dentista todavía puede aceptar o rechazar la cita. */
    public function puedeResponderse(): bool
    {
        return $this->estatus === EstatusCita::Pendiente;
    }

    /**
     * Aplica la respuesta del dentista.
     *
     * @throws ReglaNegocioException si se intenta volver a Pendiente o la cita ya fue respondida.
     */
    public function responder(EstatusCita $nuevo): self
    {
        if ($nuevo === EstatusCita::Pendiente) {
            throw new ReglaNegocioException('Una cita solo puede aceptarse o rechazarse.');
        }
        if (!$this->puedeResponderse()) {
            throw new ReglaNegocioException(
                'La cita ya fue ' . strtolower($this->estatus->value) . ' y no puede modificarse.'
            );
        }
        return new self(
            $this->id, $this->idPaciente, $this->idServicio, $this->servicioNombre,
            $this->fecha, $this->hora, $this->sintomas, $nuevo
        );
    }
}
