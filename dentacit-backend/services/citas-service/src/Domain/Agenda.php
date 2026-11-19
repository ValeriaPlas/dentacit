<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/**
 * Agregado que protege la integridad de la agenda del dentista:
 * - los bloques de horario no se empalman (RNF-05),
 * - las citas solo se agendan dentro de un bloque definido (regla Disponibilidad de Horarios),
 * - un mismo espacio no se asigna a dos citas activas.
 */
final class Agenda
{
    /**
     * @param Horario[] $horarios      Bloques de horario del dentista.
     * @param string[]  $horasOcupadas Horas (HH:MM) ya tomadas por citas Pendientes o Aceptadas
     *                                 en la fecha consultada.
     * @param int       $duracionCita  Duración de cada cita en minutos.
     */
    public function __construct(
        private readonly array $horarios,
        private readonly array $horasOcupadas = [],
        private readonly int $duracionCita = 60,
    ) {
    }

    /**
     * Verifica que un nuevo bloque de horario pueda agregarse.
     *
     * @throws ReglaNegocioException
     */
    public function validarNuevoBloque(string $fecha, string $inicio, string $fin, string $hoy): void
    {
        if (Horario::aMinutos($fin) <= Horario::aMinutos($inicio)) {
            throw new ReglaNegocioException('La hora de fin debe ser posterior a la hora de inicio.');
        }
        if ($fecha < $hoy) {
            throw new ReglaNegocioException('No se pueden crear horarios en fechas pasadas.');
        }
        foreach ($this->horarios as $h) {
            if ($h->seEmpalmaCon($fecha, $inicio, $fin)) {
                throw new ReglaNegocioException(
                    "El bloque se empalma con el horario de {$h->inicio} a {$h->fin}."
                );
            }
        }
    }

    /**
     * Verifica que una cita pueda agendarse en la fecha y hora indicadas.
     *
     * @throws ReglaNegocioException
     */
    public function validarEspacio(string $fecha, string $hora): void
    {
        $dentro = false;
        foreach ($this->horarios as $h) {
            if ($h->contiene($fecha, $hora, $this->duracionCita)) {
                $dentro = true;
                break;
            }
        }
        if (!$dentro) {
            throw new ReglaNegocioException('El dentista no atiende en ese horario. Elige uno de los horarios disponibles.');
        }
        if (in_array(substr($hora, 0, 5), $this->horasOcupadas, true)) {
            throw new ReglaNegocioException('Ese horario ya no está disponible. Elige otro por favor.');
        }
    }

    /**
     * Todos los espacios del día con su estado, para mostrar la rejilla Libre / Ocupado.
     *
     * @return array<int, array{hora:string, libre:bool}>
     */
    public function espacios(string $fecha): array
    {
        $espacios = [];
        foreach ($this->horarios as $h) {
            if ($h->fecha !== $fecha) {
                continue;
            }
            $fin = Horario::aMinutos($h->fin);
            for ($m = Horario::aMinutos($h->inicio); $m + $this->duracionCita <= $fin; $m += $this->duracionCita) {
                $hora = Horario::aHora($m);
                $espacios[$hora] = ['hora' => $hora, 'libre' => !in_array($hora, $this->horasOcupadas, true)];
            }
        }
        ksort($espacios);
        return array_values($espacios);
    }

    /**
     * Calcula los espacios libres de un día, en pasos de la duración de cita.
     *
     * @return string[] Horas de inicio disponibles (HH:MM), ordenadas.
     */
    public function espaciosDisponibles(string $fecha): array
    {
        $libres = [];
        foreach ($this->horarios as $h) {
            if ($h->fecha !== $fecha) {
                continue;
            }
            $fin = Horario::aMinutos($h->fin);
            for ($m = Horario::aMinutos($h->inicio); $m + $this->duracionCita <= $fin; $m += $this->duracionCita) {
                $hora = Horario::aHora($m);
                if (!in_array($hora, $this->horasOcupadas, true)) {
                    $libres[] = $hora;
                }
            }
        }
        sort($libres);
        return array_values(array_unique($libres));
    }
}
