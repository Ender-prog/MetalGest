<?php
require_once __DIR__ . '/clases/php/database.php';

function db() {
    return Database::getConnection();
}

function json_response($data, $status = 200) {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function input($key, $default = null) {
    return isset($_POST[$key]) ? trim((string)$_POST[$key]) : $default;
}

function int_input($key, $default = 0) {
    return isset($_POST[$key]) ? (int)$_POST[$key] : $default;
}

function required($key) {
    $value = input($key);
    if ($value === null || $value === '') {
        json_response(['success' => false, 'message' => "Falta el campo $key"], 422);
    }
    return $value;
}

function nullable_date($value) {
    return $value === '' || $value === null ? null : $value;
}

$action = $_GET['action'] ?? null;

if ($action === 'health') {
    json_response(['success' => true, 'app' => 'MetalGest']);
}

if ($action === 'login') {
    $usuario = required('usuario');
    $contrasena = required('contrasena');
    $stmt = db()->prepare(
        'SELECT u.id_usuario, u.nombre, u.apellido, u.usuario, r.nombre AS rol
         FROM usuario u
         INNER JOIN rol r ON r.id_rol = u.id_rol
         WHERE u.usuario = :usuario AND u.contrasena = :contrasena'
    );
    $stmt->execute([':usuario' => $usuario, ':contrasena' => $contrasena]);
    $user = $stmt->fetch();
    if (!$user) {
        json_response(['success' => false, 'message' => 'Credenciales incorrectas'], 401);
    }
    json_response(['success' => true, 'usuario' => $user]);
}

if ($action === 'clientes_list') {
    $rows = db()->query('SELECT * FROM cliente ORDER BY razon_social')->fetchAll();
    json_response($rows);
}

if ($action === 'pedidos_list') {
    $stmt = db()->query(
        'SELECT p.id_pedido, p.fecha, p.descripcion, p.cantidad, p.material, p.fecha_entrega,
                p.estado, c.razon_social, c.email, c.telefono
         FROM pedido p
         INNER JOIN cliente c ON c.id_cliente = p.id_cliente
         ORDER BY p.id_pedido DESC'
    );
    json_response($stmt->fetchAll());
}

if ($action === 'pedidos_create') {
    $pdo = db();
    $pdo->beginTransaction();
    try {
        $idCliente = int_input('id_cliente');
        if ($idCliente <= 0) {
            $razonSocial = required('razon_social');
            $stmtCliente = $pdo->prepare(
                'INSERT INTO cliente (razon_social, cuit, telefono, email, direccion)
                 VALUES (:razon_social, :cuit, :telefono, :email, :direccion)'
            );
            $stmtCliente->execute([
                ':razon_social' => $razonSocial,
                ':cuit' => input('cuit', ''),
                ':telefono' => input('telefono', ''),
                ':email' => input('email', ''),
                ':direccion' => input('direccion', '')
            ]);
            $idCliente = (int)$pdo->lastInsertId();
        }

        $stmtPedido = $pdo->prepare(
            'INSERT INTO pedido (id_cliente, fecha, descripcion, cantidad, material, fecha_entrega, estado)
             VALUES (:id_cliente, CURDATE(), :descripcion, :cantidad, :material, :fecha_entrega, :estado)'
        );
        $stmtPedido->execute([
            ':id_cliente' => $idCliente,
            ':descripcion' => required('descripcion'),
            ':cantidad' => int_input('cantidad', 1),
            ':material' => required('material'),
            ':fecha_entrega' => nullable_date(input('fecha_entrega')),
            ':estado' => 'Pendiente'
        ]);
        $idPedido = (int)$pdo->lastInsertId();
        $pdo->commit();
        json_response(['success' => true, 'id_pedido' => $idPedido]);
    } catch (Exception $e) {
        $pdo->rollBack();
        json_response(['success' => false, 'message' => $e->getMessage()], 500);
    }
}

if ($action === 'ordenes_list') {
    $stmt = db()->query(
        'SELECT ot.id_orden, ot.id_pedido, ot.fecha_inicio, ot.fecha_prevista, ot.fecha_finalizacion,
                ot.prioridad, ot.estado, ot.observaciones, ot.id_usuario,
                p.descripcion, p.cantidad, p.material, p.fecha_entrega,
                c.razon_social,
                COALESCE(MAX(pr.avance), 0) AS avance
         FROM orden_trabajo ot
         INNER JOIN pedido p ON p.id_pedido = ot.id_pedido
         INNER JOIN cliente c ON c.id_cliente = p.id_cliente
         LEFT JOIN produccion pr ON pr.id_orden = ot.id_orden
         GROUP BY ot.id_orden, ot.id_pedido, ot.fecha_inicio, ot.fecha_prevista, ot.fecha_finalizacion,
                  ot.prioridad, ot.estado, ot.observaciones, ot.id_usuario,
                  p.descripcion, p.cantidad, p.material, p.fecha_entrega, c.razon_social
         ORDER BY ot.id_orden DESC'
    );
    json_response($stmt->fetchAll());
}

if ($action === 'ordenes_create') {
    $pdo = db();
    $pdo->beginTransaction();
    try {
        $idPedido = int_input('id_pedido');
        $stmt = $pdo->prepare(
            'INSERT INTO orden_trabajo
                (id_pedido, fecha_inicio, fecha_prevista, prioridad, estado, observaciones, id_usuario)
             VALUES
                (:id_pedido, :fecha_inicio, :fecha_prevista, :prioridad, :estado, :observaciones, :id_usuario)'
        );
        $stmt->execute([
            ':id_pedido' => $idPedido,
            ':fecha_inicio' => nullable_date(input('fecha_inicio', date('Y-m-d'))),
            ':fecha_prevista' => nullable_date(input('fecha_prevista')),
            ':prioridad' => input('prioridad', 'Media'),
            ':estado' => 'Pendiente',
            ':observaciones' => input('observaciones', ''),
            ':id_usuario' => int_input('id_usuario', 1)
        ]);
        $idOrden = (int)$pdo->lastInsertId();
        $upd = $pdo->prepare('UPDATE pedido SET estado = :estado WHERE id_pedido = :id_pedido');
        $upd->execute([':estado' => 'Con orden de trabajo', ':id_pedido' => $idPedido]);
        $pdo->commit();
        json_response(['success' => true, 'id_orden' => $idOrden]);
    } catch (Exception $e) {
        $pdo->rollBack();
        json_response(['success' => false, 'message' => $e->getMessage()], 500);
    }
}

if ($action === 'orden_update_status') {
    $idOrden = int_input('id_orden');
    $estado = required('estado');
    $pdo = db();
    $stmt = $pdo->prepare(
        'UPDATE orden_trabajo
         SET estado = :estado,
             observaciones = CONCAT(COALESCE(observaciones, ""), :salto, :observaciones),
             fecha_finalizacion = CASE WHEN :estado_fin = "Finalizada" THEN CURDATE() ELSE fecha_finalizacion END
         WHERE id_orden = :id_orden'
    );
    $stmt->execute([
        ':estado' => $estado,
        ':estado_fin' => $estado,
        ':salto' => input('observaciones', '') === '' ? '' : "\n",
        ':observaciones' => input('observaciones', ''),
        ':id_orden' => $idOrden
    ]);
    $pedido = $pdo->prepare(
        'UPDATE pedido p
         INNER JOIN orden_trabajo ot ON ot.id_pedido = p.id_pedido
         SET p.estado = :estado
         WHERE ot.id_orden = :id_orden'
    );
    $pedido->execute([':estado' => $estado, ':id_orden' => $idOrden]);
    json_response(['success' => true]);
}

if ($action === 'produccion_list') {
    $stmt = db()->query(
        'SELECT pr.id_produccion, pr.id_orden, pr.fecha_inicio, pr.fecha_fin, pr.cantidad_producida,
                pr.avance, pr.observaciones, ot.estado AS estado_orden, p.descripcion, p.material,
                c.razon_social
         FROM produccion pr
         INNER JOIN orden_trabajo ot ON ot.id_orden = pr.id_orden
         INNER JOIN pedido p ON p.id_pedido = ot.id_pedido
         INNER JOIN cliente c ON c.id_cliente = p.id_cliente
         ORDER BY pr.id_produccion DESC'
    );
    json_response($stmt->fetchAll());
}

if ($action === 'produccion_register') {
    $idOrden = int_input('id_orden');
    $avance = max(0, min(100, int_input('avance')));
    $pdo = db();
    $pdo->beginTransaction();
    try {
        $stmt = $pdo->prepare(
            'INSERT INTO produccion (id_orden, fecha_inicio, fecha_fin, cantidad_producida, avance, observaciones)
             VALUES (:id_orden, NOW(), :fecha_fin, :cantidad_producida, :avance, :observaciones)'
        );
        $stmt->execute([
            ':id_orden' => $idOrden,
            ':fecha_fin' => $avance >= 100 ? date('Y-m-d H:i:s') : null,
            ':cantidad_producida' => int_input('cantidad_producida'),
            ':avance' => $avance,
            ':observaciones' => input('observaciones', '')
        ]);
        $estado = $avance >= 100 ? 'Finalizada' : 'En produccion';
        $updOrden = $pdo->prepare(
            'UPDATE orden_trabajo
             SET estado = :estado,
                 fecha_finalizacion = CASE WHEN :estado_fin = "Finalizada" THEN CURDATE() ELSE fecha_finalizacion END
             WHERE id_orden = :id_orden'
        );
        $updOrden->execute([':estado' => $estado, ':estado_fin' => $estado, ':id_orden' => $idOrden]);
        $updPedido = $pdo->prepare(
            'UPDATE pedido p
             INNER JOIN orden_trabajo ot ON ot.id_pedido = p.id_pedido
             SET p.estado = :estado
             WHERE ot.id_orden = :id_orden'
        );
        $updPedido->execute([':estado' => $estado, ':id_orden' => $idOrden]);
        $idProduccion = (int)$pdo->lastInsertId();
        $pdo->commit();
        json_response(['success' => true, 'id_produccion' => $idProduccion]);
    } catch (Exception $e) {
        $pdo->rollBack();
        json_response(['success' => false, 'message' => $e->getMessage()], 500);
    }
}

if ($action === 'maquinas_list') {
    $rows = db()->query('SELECT * FROM maquina ORDER BY numero_identificacion')->fetchAll();
    json_response($rows);
}

if ($action === 'mantenimiento_report') {
    $pdo = db();
    $pdo->beginTransaction();
    try {
        $stmt = $pdo->prepare(
            'INSERT INTO mantenimiento (id_maquina, id_usuario, fecha, tipo, problema, reparacion, repuestos, estado)
             VALUES (:id_maquina, :id_usuario, CURDATE(), :tipo, :problema, "", "", "Pendiente")'
        );
        $stmt->execute([
            ':id_maquina' => int_input('id_maquina'),
            ':id_usuario' => int_input('id_usuario', 3),
            ':tipo' => input('tipo', 'Correctivo'),
            ':problema' => required('problema')
        ]);
        $idMantenimiento = (int)$pdo->lastInsertId();
        $upd = $pdo->prepare('UPDATE maquina SET estado = "Con falla" WHERE id_maquina = :id_maquina');
        $upd->execute([':id_maquina' => int_input('id_maquina')]);
        $pdo->commit();
        json_response(['success' => true, 'id_mantenimiento' => $idMantenimiento]);
    } catch (Exception $e) {
        $pdo->rollBack();
        json_response(['success' => false, 'message' => $e->getMessage()], 500);
    }
}

if ($action === 'chat_send') {
    $pdo = db();
    $stmt = $pdo->prepare(
        'INSERT INTO chat_mensaje (nombre, email, mensaje, origen, departamento, estado, fecha)
         VALUES (:nombre, :email, :mensaje, :origen, :departamento, "Pendiente", NOW())'
    );
    $stmt->execute([
        ':nombre' => required('nombre'),
        ':email' => input('email', ''),
        ':mensaje' => required('mensaje'),
        ':origen' => input('origen', 'Cliente'),
        ':departamento' => input('departamento', 'Administracion')
    ]);
    json_response(['success' => true, 'id_mensaje' => (int)$pdo->lastInsertId()]);
}

if ($action === 'chat_list') {
    $stmt = db()->query(
        'SELECT id_mensaje, nombre, email, mensaje, origen, departamento, respuesta, estado,
                fecha, fecha_respuesta
         FROM chat_mensaje
         ORDER BY id_mensaje DESC'
    );
    json_response($stmt->fetchAll());
}

if ($action === 'chat_answer') {
    $stmt = db()->prepare(
        'UPDATE chat_mensaje
         SET respuesta = :respuesta, estado = "Respondido", fecha_respuesta = NOW(), id_usuario_respuesta = :id_usuario
         WHERE id_mensaje = :id_mensaje'
    );
    $stmt->execute([
        ':respuesta' => required('respuesta'),
        ':id_usuario' => int_input('id_usuario', 2),
        ':id_mensaje' => int_input('id_mensaje')
    ]);
    json_response(['success' => true]);
}
?>
<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>MetalGest | Metalurgica San Jorge</title>
    <style>
        :root {
            color-scheme: light;
            --ink: #172026;
            --muted: #5d6a73;
            --line: #d9e0e5;
            --steel: #2f5f73;
            --accent: #d08a1f;
            --soft: #f4f7f9;
            --white: #ffffff;
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            font-family: Arial, Helvetica, sans-serif;
            color: var(--ink);
            background: var(--white);
        }
        header {
            min-height: 72vh;
            display: grid;
            align-items: center;
            background:
                linear-gradient(90deg, rgba(23,32,38,.88), rgba(23,32,38,.45)),
                url("https://images.unsplash.com/photo-1504917595217-d4dc5ebe6122?auto=format&fit=crop&w=1800&q=80") center/cover;
            color: white;
            padding: 32px;
        }
        nav {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 18px 32px;
            color: white;
        }
        nav strong { font-size: 20px; }
        nav a { color: white; text-decoration: none; margin-left: 18px; }
        .hero { max-width: 780px; }
        h1 { font-size: clamp(38px, 6vw, 72px); margin: 0 0 16px; letter-spacing: 0; }
        .hero p { font-size: 20px; line-height: 1.5; max-width: 640px; }
        .button {
            display: inline-block;
            margin-top: 18px;
            padding: 13px 18px;
            background: var(--accent);
            color: white;
            border-radius: 6px;
            text-decoration: none;
            font-weight: bold;
        }
        main { background: var(--soft); }
        .section {
            max-width: 1180px;
            margin: 0 auto;
            padding: 48px 24px;
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
        }
        .service {
            background: white;
            border: 1px solid var(--line);
            border-radius: 8px;
            padding: 20px;
        }
        .service h3 { margin: 0 0 10px; }
        .service p { color: var(--muted); line-height: 1.45; }
        .chat-layout {
            display: grid;
            grid-template-columns: minmax(0, 1fr) 380px;
            gap: 28px;
            align-items: start;
        }
        .chat-panel {
            background: white;
            border: 1px solid var(--line);
            border-radius: 8px;
            padding: 18px;
        }
        label { display: block; font-weight: bold; margin: 12px 0 6px; }
        input, textarea {
            width: 100%;
            border: 1px solid var(--line);
            border-radius: 6px;
            padding: 10px;
            font: inherit;
        }
        textarea { min-height: 92px; resize: vertical; }
        button {
            margin-top: 14px;
            border: 0;
            border-radius: 6px;
            background: var(--steel);
            color: white;
            padding: 11px 14px;
            cursor: pointer;
            font-weight: bold;
        }
        .message {
            border-bottom: 1px solid var(--line);
            padding: 12px 0;
        }
        .message:last-child { border-bottom: 0; }
        .message small { color: var(--muted); display: block; margin-top: 4px; }
        .reply {
            margin-top: 8px;
            padding: 8px;
            border-left: 3px solid var(--accent);
            background: #fff8eb;
        }
        @media (max-width: 820px) {
            header { padding: 84px 22px 32px; }
            nav { padding: 16px 22px; }
            .chat-layout { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <nav>
        <strong>MetalGest</strong>
        <div>
            <a href="#servicios">Servicios</a>
            <a href="#chat">Chat</a>
        </div>
    </nav>
    <header>
        <section class="hero">
            <h1>Metalurgica San Jorge</h1>
            <p>Fabricacion, mecanizado y seguimiento de pedidos con comunicacion directa entre clientes, administracion y produccion.</p>
            <a class="button" href="#chat">Contactar administracion</a>
        </section>
    </header>
    <main>
        <section class="section" id="servicios">
            <h2>Servicios metalurgicos</h2>
            <div class="grid">
                <article class="service">
                    <h3>Fabricacion a medida</h3>
                    <p>Piezas, estructuras y componentes segun planos o requerimientos tecnicos.</p>
                </article>
                <article class="service">
                    <h3>Gestion de pedidos</h3>
                    <p>Seguimiento administrativo de acuerdos, ordenes de trabajo y fechas de entrega.</p>
                </article>
                <article class="service">
                    <h3>Control de avance</h3>
                    <p>Produccion registra tiempos, cantidades y observaciones para mantener trazabilidad.</p>
                </article>
            </div>
        </section>
        <section class="section chat-layout" id="chat">
            <div>
                <h2>Chat con Administracion</h2>
                <p>Los mensajes enviados desde esta landing quedan registrados en la base de datos y pueden responderse desde el modulo de Administracion.</p>
                <div id="chatMessages"></div>
            </div>
            <form class="chat-panel" id="chatForm">
                <label for="nombre">Nombre o empresa</label>
                <input id="nombre" name="nombre" required>
                <label for="email">Email</label>
                <input id="email" name="email" type="email">
                <label for="mensaje">Mensaje</label>
                <textarea id="mensaje" name="mensaje" required></textarea>
                <button type="submit">Enviar mensaje</button>
                <p id="chatStatus"></p>
            </form>
        </section>
    </main>
    <script>
        const form = document.getElementById('chatForm');
        const statusBox = document.getElementById('chatStatus');
        const messages = document.getElementById('chatMessages');

        async function loadChat() {
            const response = await fetch('index.php?action=chat_list');
            const data = await response.json();
            messages.innerHTML = data.slice(0, 8).map(item => `
                <article class="message">
                    <strong>${escapeHtml(item.nombre)}</strong>
                    <small>${escapeHtml(item.fecha || '')} - ${escapeHtml(item.estado || '')}</small>
                    <p>${escapeHtml(item.mensaje)}</p>
                    ${item.respuesta ? `<div class="reply"><strong>Administracion:</strong> ${escapeHtml(item.respuesta)}</div>` : ''}
                </article>
            `).join('');
        }

        function escapeHtml(value) {
            return String(value || '').replace(/[&<>"']/g, char => ({
                '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
            }[char]));
        }

        form.addEventListener('submit', async event => {
            event.preventDefault();
            const body = new URLSearchParams(new FormData(form));
            const response = await fetch('index.php?action=chat_send', { method: 'POST', body });
            const data = await response.json();
            statusBox.textContent = data.success ? 'Mensaje enviado a Administracion.' : 'No se pudo enviar el mensaje.';
            if (data.success) {
                form.reset();
                loadChat();
            }
        });

        loadChat();
        setInterval(loadChat, 15000);
    </script>
</body>
</html>
