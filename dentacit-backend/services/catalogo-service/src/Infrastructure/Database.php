<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Infrastructure;

use PDO;

/** Crea la conexión PDO a MySQL con los datos de las variables de entorno. */
final class Database
{
    public static function conectar(): PDO
    {
        $host = getenv('DB_HOST') ?: '127.0.0.1';
        $puerto = getenv('DB_PORT') ?: '3306';
        $nombre = getenv('DB_NAME') ?: 'dentacit';
        $dsn = "mysql:host=$host;port=$puerto;dbname=$nombre;charset=utf8mb4";

        $pdo = new PDO($dsn, getenv('DB_USER') ?: 'dentacit', getenv('DB_PASS') ?: '', [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);
        $pdo->exec("SET time_zone = '-06:00'");
        return $pdo;
    }
}
