<?php
declare(strict_types=1);

use Dentacit\Citas\Domain\Agenda;
use Dentacit\Citas\Domain\Horario;
use Dentacit\Citas\Domain\ReglaNegocioException;
use PHPUnit\Framework\TestCase;

/** Reglas del agregado Agenda (RNF-05 y Disponibilidad de Horarios). */
final class AgendaTest extends TestCase
{
    private function agenda(array $ocupadas = []): Agenda
    {
        return new Agenda([new Horario(1, '2026-12-01', '09:00', '13:00')], $ocupadas, 60);
    }

    public function testBloqueEmpalmadoEsRechazado(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->agenda()->validarNuevoBloque('2026-12-01', '12:00', '14:00', '2026-11-18');
    }

    public function testBloqueContiguoEsValido(): void
    {
        $this->agenda()->validarNuevoBloque('2026-12-01', '13:00', '15:00', '2026-11-18');
        $this->addToAssertionCount(1);
    }

    public function testBloqueConFinAntesDelInicioEsRechazado(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->agenda()->validarNuevoBloque('2026-12-02', '15:00', '14:00', '2026-11-18');
    }

    public function testBloqueEnFechaPasadaEsRechazado(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->agenda()->validarNuevoBloque('2026-11-01', '09:00', '10:00', '2026-11-18');
    }

    public function testEspaciosDisponiblesExcluyenLosOcupados(): void
    {
        $this->assertSame(['09:00', '11:00', '12:00'], $this->agenda(['10:00'])->espaciosDisponibles('2026-12-01'));
    }

    public function testCitaFueraDelHorarioEsRechazada(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->agenda()->validarEspacio('2026-12-01', '12:30'); // terminaría 13:30
    }

    public function testCitaEnHoraOcupadaEsRechazada(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->agenda(['10:00'])->validarEspacio('2026-12-01', '10:00');
    }

    public function testLaRejillaMarcaLibresYOcupados(): void
    {
        $this->assertSame([
            ['hora' => '09:00', 'libre' => true],
            ['hora' => '10:00', 'libre' => false],
            ['hora' => '11:00', 'libre' => true],
            ['hora' => '12:00', 'libre' => true],
        ], $this->agenda(['10:00'])->espacios('2026-12-01'));
    }
}
