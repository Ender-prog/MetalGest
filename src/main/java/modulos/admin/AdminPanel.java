package modulos.admin;

import modulos.common.ApiClient;
import modulos.common.JsonUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminPanel {
    private static int usuarioId = 1;

    public static void openAdmin() {
        openAdmin(1);
    }

    public static void openAdmin(int idUsuario) {
        usuarioId = idUsuario;
        SwingUtilities.invokeLater(AdminPanel::cargarMain);
    }

    public static void cargarMain() {
        JFrame ventana = new JFrame("MetalGest - Administracion");
        ventana.setSize(1050, 650);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Pedidos", crearPanelPedidos());
        tabs.addTab("Ordenes de trabajo", crearPanelOrdenes());
        tabs.addTab("Chat clientes", crearPanelChat());
        ventana.add(tabs);

        ventana.setVisible(true);
    }

    private static JPanel crearPanelPedidos() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Cliente", "Descripcion", "Cantidad", "Material", "Entrega", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 6, 8, 8));
        JTextField cliente = new JTextField();
        JTextField email = new JTextField();
        JTextField telefono = new JTextField();
        JTextField descripcion = new JTextField();
        JTextField cantidad = new JTextField("1");
        JTextField material = new JTextField();
        JTextField entrega = new JTextField("2026-10-15");
        form.add(new JLabel("Cliente"));
        form.add(new JLabel("Email"));
        form.add(new JLabel("Telefono"));
        form.add(new JLabel("Descripcion"));
        form.add(new JLabel("Cantidad"));
        form.add(new JLabel("Material"));
        form.add(cliente);
        form.add(email);
        form.add(telefono);
        form.add(descripcion);
        form.add(cantidad);
        form.add(material);

        JPanel inferior = new JPanel(new BorderLayout(8, 8));
        inferior.add(form, BorderLayout.CENTER);
        JPanel acciones = new JPanel();
        acciones.add(new JLabel("Entrega"));
        acciones.add(entrega);
        JButton crearPedido = new JButton("Crear pedido");
        JButton crearOrden = new JButton("Generar orden");
        JButton recargar = new JButton("Recargar");
        acciones.add(crearPedido);
        acciones.add(crearOrden);
        acciones.add(recargar);
        inferior.add(acciones, BorderLayout.SOUTH);
        panel.add(inferior, BorderLayout.SOUTH);

        recargarPedidos(modelo);

        recargar.addActionListener(e -> recargarPedidos(modelo));
        crearPedido.addActionListener(e -> {
            if (cliente.getText().trim().isEmpty() || descripcion.getText().trim().isEmpty() || material.getText().trim().isEmpty()) {
                mostrarAviso("Completa cliente, descripcion y material.");
                return;
            }
            Map<String, String> data = new LinkedHashMap<>();
            data.put("razon_social", cliente.getText().trim());
            data.put("email", email.getText().trim());
            data.put("telefono", telefono.getText().trim());
            data.put("descripcion", descripcion.getText().trim());
            data.put("cantidad", cantidad.getText().trim());
            data.put("material", material.getText().trim());
            data.put("fecha_entrega", entrega.getText().trim());
            if (postOk("pedidos_create", data, "Pedido creado.")) {
                cliente.setText("");
                email.setText("");
                telefono.setText("");
                descripcion.setText("");
                cantidad.setText("1");
                material.setText("");
                recargarPedidos(modelo);
            }
        });

        crearOrden.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0) {
                mostrarAviso("Selecciona un pedido para generar la orden.");
                return;
            }
            int idPedido = Integer.parseInt(modelo.getValueAt(row, 0).toString());
            JTextField fechaInicio = new JTextField(java.time.LocalDate.now().toString());
            JTextField fechaPrevista = new JTextField(entrega.getText().trim());
            JComboBox<String> prioridad = new JComboBox<>(new String[]{"Media", "Alta", "Baja"});
            JTextArea obs = new JTextArea(4, 24);
            JPanel dialog = new JPanel(new GridLayout(0, 1, 6, 6));
            dialog.add(new JLabel("Fecha de inicio"));
            dialog.add(fechaInicio);
            dialog.add(new JLabel("Fecha prevista"));
            dialog.add(fechaPrevista);
            dialog.add(new JLabel("Prioridad"));
            dialog.add(prioridad);
            dialog.add(new JLabel("Observaciones"));
            dialog.add(new JScrollPane(obs));
            int result = JOptionPane.showConfirmDialog(null, dialog, "Nueva orden de trabajo", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                Map<String, String> data = new LinkedHashMap<>();
                data.put("id_pedido", String.valueOf(idPedido));
                data.put("fecha_inicio", fechaInicio.getText().trim());
                data.put("fecha_prevista", fechaPrevista.getText().trim());
                data.put("prioridad", prioridad.getSelectedItem().toString());
                data.put("observaciones", obs.getText().trim());
                data.put("id_usuario", String.valueOf(usuarioId));
                if (postOk("ordenes_create", data, "Orden generada para Produccion.")) {
                    recargarPedidos(modelo);
                }
            }
        });

        return panel;
    }

    private static JPanel crearPanelOrdenes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Pedido", "Cliente", "Material", "Cantidad", "Prioridad", "Estado", "Avance", "Prevista"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel acciones = new JPanel();
        JComboBox<String> estado = new JComboBox<>(new String[]{"Pendiente", "En produccion", "Pausada", "Finalizada"});
        JTextField observaciones = new JTextField(28);
        JButton actualizar = new JButton("Actualizar estado");
        JButton recargar = new JButton("Recargar");
        acciones.add(new JLabel("Estado"));
        acciones.add(estado);
        acciones.add(new JLabel("Observacion"));
        acciones.add(observaciones);
        acciones.add(actualizar);
        acciones.add(recargar);
        panel.add(acciones, BorderLayout.SOUTH);

        recargarOrdenes(modelo);

        recargar.addActionListener(e -> recargarOrdenes(modelo));
        actualizar.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0) {
                mostrarAviso("Selecciona una orden.");
                return;
            }
            Map<String, String> data = new LinkedHashMap<>();
            data.put("id_orden", modelo.getValueAt(row, 0).toString());
            data.put("estado", estado.getSelectedItem().toString());
            data.put("observaciones", observaciones.getText().trim());
            if (postOk("orden_update_status", data, "Estado actualizado y cliente notificado en el pedido.")) {
                observaciones.setText("");
                recargarOrdenes(modelo);
            }
        });

        return panel;
    }

    private static JPanel crearPanelChat() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Cliente", "Email", "Mensaje", "Estado", "Respuesta"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new BorderLayout(8, 8));
        JTextArea respuesta = new JTextArea(4, 50);
        JButton responder = new JButton("Responder");
        JButton recargar = new JButton("Recargar");
        JPanel botones = new JPanel();
        botones.add(responder);
        botones.add(recargar);
        acciones.add(new JScrollPane(respuesta), BorderLayout.CENTER);
        acciones.add(botones, BorderLayout.EAST);
        panel.add(acciones, BorderLayout.SOUTH);

        recargarChat(modelo);

        recargar.addActionListener(e -> recargarChat(modelo));
        responder.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0) {
                mostrarAviso("Selecciona un mensaje.");
                return;
            }
            if (respuesta.getText().trim().isEmpty()) {
                mostrarAviso("Escribe una respuesta para el cliente.");
                return;
            }
            Map<String, String> data = new LinkedHashMap<>();
            data.put("id_mensaje", modelo.getValueAt(row, 0).toString());
            data.put("respuesta", respuesta.getText().trim());
            data.put("id_usuario", String.valueOf(usuarioId));
            if (postOk("chat_answer", data, "Respuesta registrada.")) {
                respuesta.setText("");
                recargarChat(modelo);
            }
        });

        return panel;
    }

    private static void recargarPedidos(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        for (Map<String, String> row : getRows("pedidos_list")) {
            modelo.addRow(new Object[]{
                    row.get("id_pedido"),
                    row.get("razon_social"),
                    row.get("descripcion"),
                    row.get("cantidad"),
                    row.get("material"),
                    row.get("fecha_entrega"),
                    row.get("estado")
            });
        }
    }

    private static void recargarOrdenes(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        for (Map<String, String> row : getRows("ordenes_list")) {
            modelo.addRow(new Object[]{
                    row.get("id_orden"),
                    row.get("id_pedido"),
                    row.get("razon_social"),
                    row.get("material"),
                    row.get("cantidad"),
                    row.get("prioridad"),
                    row.get("estado"),
                    row.get("avance") + "%",
                    row.get("fecha_prevista")
            });
        }
    }

    private static void recargarChat(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        for (Map<String, String> row : getRows("chat_list")) {
            modelo.addRow(new Object[]{
                    row.get("id_mensaje"),
                    row.get("nombre"),
                    row.get("email"),
                    row.get("mensaje"),
                    row.get("estado"),
                    row.get("respuesta")
            });
        }
    }

    private static List<Map<String, String>> getRows(String action) {
        try {
            return JsonUtil.parseArray(ApiClient.get(action));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar con MetalGest: " + e.getMessage(), "Conexion", JOptionPane.ERROR_MESSAGE);
            return java.util.Collections.emptyList();
        }
    }

    private static boolean postOk(String action, Map<String, String> data, String okMessage) {
        try {
            String response = ApiClient.post(action, data);
            if (ApiClient.isSuccess(response)) {
                JOptionPane.showMessageDialog(null, okMessage, "MetalGest", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            JOptionPane.showMessageDialog(null, "La operacion no pudo completarse: " + response, "MetalGest", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar con MetalGest: " + e.getMessage(), "Conexion", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private static void mostrarAviso(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, "MetalGest", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        openAdmin();
    }
}
