CREATE DATABASE empresa;
USE empresa;

-- =========================
-- EMPRESAS / CLIENTES
-- =========================
CREATE TABLE empresas (
    id_empresa INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    telefono VARCHAR(30),
    contacto VARCHAR(100)
);

-- =========================
-- PEDIDOS
-- =========================
CREATE TABLE pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NOT NULL,
    materiales_ofrecidos VARCHAR(255),
    acciones VARCHAR(255),
    estado VARCHAR(50),
    fecha_programada DATE,

    FOREIGN KEY (id_empresa)
        REFERENCES empresas(id_empresa)
);

-- =========================
-- RECURSOS / MATERIALES
-- =========================
CREATE TABLE recursos (
    id_material INT AUTO_INCREMENT PRIMARY KEY,
    materiales_especificos VARCHAR(150),
    proveedor VARCHAR(100),
    cantidad INT,
    unidad VARCHAR(30)
);

-- =========================
-- ORDENES DE TRABAJO
-- =========================
CREATE TABLE ordenes_trabajo (
    id_orden INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    descripcion_trabajo TEXT,
    materiales_especificos TEXT,
    proveedor VARCHAR(100),
    sector VARCHAR(100),
    observaciones TEXT,

    FOREIGN KEY (id_pedido)
        REFERENCES pedidos(id_pedido)
);

-- =========================
-- EMPLEADOS
-- =========================
CREATE TABLE empleados (
    id_empleado INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT,
    direccion VARCHAR(150),
    estado VARCHAR(50),
    fecha_alta DATE,

    FOREIGN KEY (id_pedido)
        REFERENCES pedidos(id_pedido)
);

-- =========================
-- MAQUINAS
-- =========================
CREATE TABLE maquinas (
    id_maquina INT AUTO_INCREMENT PRIMARY KEY,
    modelo VARCHAR(100),
    proveedor VARCHAR(100),
    costo DECIMAL(12,2),
    condicion VARCHAR(50),
    estado VARCHAR(50),
    horas_funcionamiento DECIMAL(10,2),
    mantenimiento TEXT
);

-- =========================
-- MANTENIMIENTO
-- =========================
CREATE TABLE mantenimiento (
    id_mantenimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_maquina INT NOT NULL,
    id_empleado INT,
    fecha_mantenimiento DATE,
    observaciones TEXT,
    fallas TEXT,
    repuestos_utilizados TEXT,

    FOREIGN KEY (id_maquina)
        REFERENCES maquinas(id_maquina),

    FOREIGN KEY (id_empleado)
        REFERENCES empleados(id_empleado)
);

-- =========================
-- COMPRAS
-- =========================
CREATE TABLE compras (
    id_compra INT AUTO_INCREMENT PRIMARY KEY,
    fecha_compra DATE,
    id_material INT,
    proveedor VARCHAR(100),
    precio DECIMAL(12,2),
    cantidad INT,

    FOREIGN KEY (id_material)
        REFERENCES recursos(id_material)
);

-- =========================
-- EQUIPOS
-- =========================
CREATE TABLE equipos (
    id_equipo INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT,
    direccion VARCHAR(150),
    estado VARCHAR(50),
    fecha_alta DATE,

    FOREIGN KEY (id_pedido)
        REFERENCES pedidos(id_pedido)
);