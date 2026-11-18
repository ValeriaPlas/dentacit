<?php
declare(strict_types=1);

use Dentacit\Citas\Domain\Cita;
use Dentacit\Citas\Domain\EstatusCita;
use Dentacit\Citas\Domain\ReglaNegocioException;
use PHPUnit\Framework\TestCase;

/** Reglas de la entidad Cita. */
final class CitaTest extends TestCase
{
    private function cita(EstatusCita $estatus = EstatusCita::Pendiente): Cita
    {
        return new Cita(1, 1, 1, 'Limpieza', new DateTimeImmutable('2026-12-01'), '10:00', '', $estatus);
    }

    public function testUnaSolicitudNuevaQuedaPendiente(): void
    {
        $c = Cita::solicitar(1, 1, 'Limpieza', new DateTimeImmutable('2026-12-01'), '10:00', ' dolor ');
        $this->assertSame(EstatusCita::Pendiente, $c->estatus);
        $this->assertSame('dolor', $c->sintomas);
    }

    public function testUnaCitaPendienteSePuedeAceptar(): void
    {
        $this->assertSame(EstatusCita::Aceptada, $this->cita()->responder(EstatusCita::Aceptada)->estatus);
    }

    public function testUnaCitaRespondidaNoSeModifica(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->cita(EstatusCita::Rechazada)->responder(EstatusCita::Aceptada);
    }

    public function testNoSePuedeRegresarAPendiente(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->cita()->responder(EstatusCita::Pendiente);
    }

    public function testEstatusDesconocidoEsRechazado(): void
    {
        $this->expectException(ReglaNegocioException::class);
        EstatusCita::desdeTexto('Cancelada');
    }
}
