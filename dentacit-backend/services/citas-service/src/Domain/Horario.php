<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/**
 * Bloque de horario en el que el dentista atiende.
 * Las horas se manejan en minutos desde medianoche para comparar fácilmente.
 */
final class Horario
{
    public function __construct(
        public readonly ?int $id,
        public readonly string $fecha,   // AAAA-MM-DD
        public readonly string $inicio,  // HH:MM
        public readonly string $fin,     // HH:MM
    ) {
    }

    /** Convierte "HH:MM" (o "HH:MM:SS") a minutos desde medianoche. */
    public static function aMinutos(string $hora): int
    {
        [$h, $m] = array_map('intval', explode(':', $hora));
        return $h * 60 + $m;
    }

    /** Convierte minutos desde medianoche a "HH:MM". */
    public static function aHora(int $minutos): string
    {
        return sprintf('%02d:%02d', intdiv($minutos, 60), $minutos % 60);
    }

    /** Indica si este bloque comparte al menos un minuto con otro intervalo del mismo día. */
    public function seEmpalmaCon(string $fecha, string $inicio, string $fin): bool
    {
        return $this->fecha === $fecha
            && self::aMinutos($inicio) < self::aMinutos($this->fin)
            && self::aMinutos($this->inicio) < self::aMinutos($fin);
    }

    /** Indica si un intervalo [inicio, inicio + duración) cabe completo dentro del bloque. */
    public function contiene(string $fecha, string $inicio, int $duracionMin): bool
    {
        $desde = self::aMinutos($inicio);
        return $this->fecha === $fecha
            && $desde >= self::aMinutos($this->inicio)
            && $desde + $duracionMin <= self::aMinutos($this->fin);
    }
}
