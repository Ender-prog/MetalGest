
CREATE DATABASE IF NOT EXISTS biblioteca;
USE biblioteca;

CREATE TABLE IF NOT EXISTS libros (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    cantidad INT NOT NULL
);

-- Tabla simple de usuarios con rol 'admin' o 'cliente'
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    rol ENUM('admin','cliente') NOT NULL DEFAULT 'cliente'
);

-- Datos de ejemplo
INSERT INTO usuarios (username, rol) VALUES ('admin', 'admin') ON DUPLICATE KEY UPDATE rol = VALUES(rol);
INSERT INTO usuarios (username, rol) VALUES ('juan', 'cliente') ON DUPLICATE KEY UPDATE rol = VALUES(rol);
