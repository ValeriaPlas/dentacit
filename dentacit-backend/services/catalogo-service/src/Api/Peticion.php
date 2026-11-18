<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Api;

/** Datos de la petición HTTP actual. */
final class Peticion
{
    /**
     * @param array<string, mixed>  $query
     * @param array<string, mixed>  $cuerpo
     * @param array<string, string> $encabezados  (claves en minúsculas)
     */
    public function __construct(
        public readonly string $metodo,
        public readonly string $ruta,
        public readonly array $query,
        public readonly array $cuerpo,
        public readonly array $encabezados,
    ) {
    }

    /** Construye la petición a partir de las variables globales de PHP. */
    public static function desdeGlobales(): self
    {
        $encabezados = [];
        foreach (function_exists('getallheaders') ? getallheaders() : [] as $k => $v) {
            $encabezados[strtolower($k)] = $v;
        }
        if (!isset($encabezados['authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
            $encabezados['authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
        }
        $cuerpo = json_decode((string)file_get_contents('php://input'), true);

        return new self(
            strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET'),
            rtrim(parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/', '/') ?: '/',
            $_GET,
            is_array($cuerpo) ? $cuerpo : [],
            $encabezados
        );
    }

    /** Token Bearer del encabezado Authorization, o null. */
    public function token(): ?string
    {
        $auth = $this->encabezados['authorization'] ?? '';
        return preg_match('/^Bearer\s+(\S+)$/i', $auth, $m) ? $m[1] : null;
    }
}
