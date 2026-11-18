<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Api;

/** Enrutador mínimo: asocia método + expresión regular de ruta con un manejador. */
final class Router
{
    /** @var array<int, array{0:string, 1:string, 2:callable}> */
    private array $rutas = [];

    public function agregar(string $metodo, string $patron, callable $manejador): void
    {
        $this->rutas[] = [$metodo, '#^' . $patron . '$#', $manejador];
    }

    /**
     * @return array{0:int, 1:mixed} Código HTTP y cuerpo a responder.
     */
    public function despachar(Peticion $peticion): array
    {
        $rutaExiste = false;
        foreach ($this->rutas as [$metodo, $regex, $manejador]) {
            if (!preg_match($regex, $peticion->ruta, $coincidencias)) {
                continue;
            }
            $rutaExiste = true;
            if ($metodo !== $peticion->metodo) {
                continue;
            }
            $parametros = array_slice($coincidencias, 1);
            return $manejador($peticion, ...$parametros);
        }
        return $rutaExiste
            ? [405, ['ok' => false, 'msg' => 'Método no permitido.']]
            : [404, ['ok' => false, 'msg' => 'Ruta no encontrada.']];
    }
}
