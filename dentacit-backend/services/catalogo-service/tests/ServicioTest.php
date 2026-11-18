<?php
declare(strict_types=1);

use Dentacit\Catalogo\Application\GestionarCatalogo;
use Dentacit\Catalogo\Domain\NoEncontradoException;
use Dentacit\Catalogo\Domain\ReglaNegocioException;
use Dentacit\Catalogo\Domain\Servicio;
use Dentacit\Catalogo\Domain\ServicioRepository;
use PHPUnit\Framework\TestCase;

/** Reglas del catálogo de servicios (RF-07). */
final class ServicioTest extends TestCase
{
    private function repo(): ServicioRepository
    {
        return new class implements ServicioRepository {
            /** @var array<int, Servicio> */
            public array $datos = [];
            public array $eliminados = [];

            public function listar(bool $incluirInactivos): array
            {
                return array_values(array_filter($this->datos, fn(Servicio $s) => $incluirInactivos || $s->activo));
            }

            public function obtener(int $id): ?Servicio
            {
                return $this->datos[$id] ?? null;
            }

            public function crear(Servicio $servicio): Servicio
            {
                $nuevo = $servicio->conId(count($this->datos) + 1);
                $this->datos[$nuevo->id] = $nuevo;
                return $nuevo;
            }

            public function actualizar(Servicio $servicio): bool
            {
                $this->datos[$servicio->id] = $servicio;
                return true;
            }

            public function eliminar(int $id): bool
            {
                if (!isset($this->datos[$id])) {
                    return false;
                }
                unset($this->datos[$id]);
                return true;
            }
        };
    }

    public function testServicioSinNombreEsInvalido(): void
    {
        $this->expectException(ReglaNegocioException::class);
        new Servicio(null, '  ', '', 100);
    }

    public function testServicioConCostoCeroOTextoEsInvalido(): void
    {
        foreach ([0, -5, 'abc', null] as $costo) {
            try {
                new Servicio(null, 'Resina', '', $costo);
                $this->fail('Debió rechazar el costo ' . var_export($costo, true));
            } catch (ReglaNegocioException) {
                $this->addToAssertionCount(1);
            }
        }
    }

    public function testElCostoSeRedondeaADosDecimales(): void
    {
        $this->assertSame(850.13, (new Servicio(null, 'Resina', '', '850.129'))->costo);
    }

    public function testElPacienteSoloVeServiciosActivos(): void
    {
        $catalogo = new GestionarCatalogo($repo = $this->repo());
        $catalogo->crear(['nombre' => 'Limpieza', 'costo' => 600]);
        $catalogo->crear(['nombre' => 'Blanqueamiento', 'costo' => 3500, 'activo' => false]);

        $this->assertCount(1, $catalogo->listar(false));
        $this->assertCount(2, $catalogo->listar(true));
    }

    public function testActualizarServicioInexistenteDa404(): void
    {
        $this->expectException(NoEncontradoException::class);
        (new GestionarCatalogo($this->repo()))->actualizar(9, ['nombre' => 'X', 'costo' => 10]);
    }

    public function testEliminarServicio(): void
    {
        $catalogo = new GestionarCatalogo($this->repo());
        $s = $catalogo->crear(['nombre' => 'Limpieza', 'costo' => 600]);
        $catalogo->eliminar($s->id);
        $this->assertCount(0, $catalogo->listar(true));
    }
}
