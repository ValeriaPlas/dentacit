<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/** Contrato de persistencia de los bloques de horario. */
interface HorarioRepository
{
    /** @return Horario[] */
    public function listar(?string $desde = null): array;

    /** @return Horario[] */
    public function listarPorFecha(string $fecha): array;

    public function crear(Horario $horario, int $idDentista): Horario;

    public function eliminar(int $id): bool;
}
