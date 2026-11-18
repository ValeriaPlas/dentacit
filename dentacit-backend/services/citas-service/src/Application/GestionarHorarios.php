<?php
declare(strict_types=1);

namespace Dentacit\Citas\Application;

use DateTimeImmutable;
use Dentacit\Citas\Domain\Agenda;
use Dentacit\Citas\Domain\CitaRepository;
use Dentacit\Citas\Domain\Horario;
use Dentacit\Citas\Domain\HorarioRepository;
use Dentacit\Citas\Domain\NoEncontradoException;
use Dentacit\Citas\Domain\ReglaNegocioException;

/**
 * Casos de uso de la agenda: crear y eliminar bloques de horario,
 * y consultar los espacios disponibles para el paciente.
 */
final class GestionarHorarios
{
    public function __construct(
        private readonly HorarioRepository $horarios,
        private readonly CitaRepository $citas,
        private readonly int $duracionCita = 60,
    ) {
    }

    /**
     * Crea un bloque de horario validando la agenda.
     *
     * @param array<string, mixed> $datos fecha, hora_inicio, hora_fin.
     */
    public function crear(array $datos, int $idDentista, string $hoy): Horario
    {
        $fecha = trim((string)($datos['fecha'] ?? ''));
        $inicio = substr(trim((string)($datos['hora_inicio'] ?? '')), 0, 5);
        $fin = substr(trim((string)($datos['hora_fin'] ?? '')), 0, 5);

        $f = DateTimeImmutable::createFromFormat('!Y-m-d', $fecha);
        if ($f === false || $f->format('Y-m-d') !== $fecha) {
            throw new ReglaNegocioException('La fecha debe tener el formato AAAA-MM-DD.');
        }
        foreach (['inicio' => $inicio, 'fin' => $fin] as $campo => $valor) {
            if (!preg_match('/^([01]\d|2[0-3]):[0-5]\d$/', $valor)) {
                throw new ReglaNegocioException("La hora de $campo debe tener el formato HH:MM.");
            }
        }

        (new Agenda($this->horarios->listarPorFecha($fecha)))->validarNuevoBloque($fecha, $inicio, $fin, $hoy);
        return $this->horarios->crear(new Horario(null, $fecha, $inicio, $fin), $idDentista);
    }

    public function eliminar(int $id): void
    {
        if (!$this->horarios->eliminar($id)) {
            throw new NoEncontradoException("El horario $id no existe.");
        }
    }

    /**
     * Espacios libres de un día para el formulario del paciente.
     *
     * @return string[]
     */
    public function disponibles(string $fecha, string $hoy, ?string $ahora = null): array
    {
        $f = DateTimeImmutable::createFromFormat('!Y-m-d', $fecha);
        if ($f === false || $f->format('Y-m-d') !== $fecha) {
            throw new ReglaNegocioException('La fecha debe tener el formato AAAA-MM-DD.');
        }
        if ($fecha < $hoy) {
            return [];
        }
        $agenda = new Agenda(
            $this->horarios->listarPorFecha($fecha),
            $this->citas->horasOcupadas($fecha),
            $this->duracionCita
        );
        $libres = $agenda->espaciosDisponibles($fecha);
        // Hoy solo se ofrecen las horas que aún no pasan.
        if ($fecha === $hoy && $ahora !== null) {
            $libres = array_values(array_filter($libres, fn(string $h) => $h > $ahora));
        }
        return $libres;
    }

    /**
     * Próximos días con espacios libres, para que el paciente elija sin adivinar fechas.
     *
     * @return array<int, array{fecha:string, horas:string[]}>
     */
    public function proximos(string $hoy, string $ahora, int $dias = 30): array
    {
        $limite = (new DateTimeImmutable($hoy))->modify("+$dias days")->format('Y-m-d');
        $fechas = [];
        foreach ($this->horarios->listar($hoy) as $h) {
            if ($h->fecha <= $limite) {
                $fechas[$h->fecha] = true;
            }
        }
        ksort($fechas);

        $resultado = [];
        foreach (array_keys($fechas) as $fecha) {
            $horas = $this->disponibles($fecha, $hoy, $ahora);
            if ($horas !== []) {
                $resultado[] = ['fecha' => $fecha, 'horas' => $horas];
            }
        }
        return $resultado;
    }
}
